import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Html5Qrcode } from 'html5-qrcode';
import { UsersService } from '@core/services/users.service';

const READER_ID = 'admin-qr-reader';

export interface QrIdentityPreview {
  userId: number | null;
  rows: { label: string; value: string }[];
}

@Component({
  selector: 'app-admin-user-scan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-user-scan.component.html'
})
export class AdminUserScanComponent implements OnDestroy {
  errorMessage = '';
  hintMessage = '';
  manualInput = '';
  identityPreview: QrIdentityPreview | null = null;
  resolvingUserId = false;
  cameraActive = false;
  starting = false;

  private scanner: Html5Qrcode | null = null;
  private handled = false;

  constructor(
    private router: Router,
    private usersService: UsersService
  ) {}

  ngOnDestroy(): void {
    void this.stopCamera();
  }

  async startCamera(): Promise<void> {
    this.errorMessage = '';
    this.hintMessage = '';
    this.identityPreview = null;
    this.resolvingUserId = false;
    this.handled = false;
    this.starting = true;
    try {
      if (!this.scanner) {
        this.scanner = new Html5Qrcode(READER_ID);
      }
      await this.scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 260, height: 260 } },
        (decodedText) => {
          this.onDecoded(decodedText);
        },
        () => {}
      );
      this.cameraActive = true;
    } catch (e) {
      this.errorMessage =
        e instanceof Error
          ? e.message
          : 'Could not start the camera. Allow permission or use manual entry below.';
      this.cameraActive = false;
    } finally {
      this.starting = false;
    }
  }

  async stopCamera(): Promise<void> {
    if (!this.scanner) {
      return;
    }
    try {
      if (this.cameraActive) {
        await this.scanner.stop();
      }
    } catch {
      /* ignore */
    }
    try {
      this.scanner.clear();
    } catch {
      /* ignore */
    }
    this.cameraActive = false;
  }

  decodeManual(): void {
    this.errorMessage = '';
    this.hintMessage = '';
    const preview = this.parseIdentity(this.manualInput.trim());
    if (!preview || preview.rows.length === 0) {
      this.identityPreview = null;
      this.errorMessage =
        'Could not read identity data. Paste the full text from the QR, or only the numeric user id.';
      return;
    }
    this.identityPreview = preview;
    this.maybeResolveUserIdFromEmail(preview);
  }

  openUserProfile(): void {
    const id = this.identityPreview?.userId;
    if (id == null || this.resolvingUserId) {
      return;
    }
    this.hintMessage = `Opening user #${id}…`;
    void this.router.navigate(['/admin/users', id]);
  }

  clearPreview(): void {
    this.identityPreview = null;
    this.resolvingUserId = false;
    this.errorMessage = '';
    this.hintMessage = '';
  }

  private onDecoded(text: string): void {
    if (this.handled) {
      return;
    }
    const preview = this.parseIdentity(text);
    if (!preview || preview.rows.length === 0) {
      return;
    }
    this.handled = true;
    this.identityPreview = preview;
    this.errorMessage = '';
    this.hintMessage = '';
    this.maybeResolveUserIdFromEmail(preview);
    void this.stopCamera();
  }

  /**
   * New CivicIdentity/v2 QRs omit user id — resolve account by email (admin API).
   */
  private maybeResolveUserIdFromEmail(preview: QrIdentityPreview): void {
    if (preview.userId != null) {
      this.resolvingUserId = false;
      return;
    }
    const email = this.extractEmailFromRows(preview.rows);
    if (!email) {
      this.errorMessage =
        'This payload has no user id and no email line — paste a full CivicIdentity QR, legacy JSON, or a numeric id.';
      this.resolvingUserId = false;
      return;
    }
    this.resolvingUserId = true;
    this.usersService.getUserByEmailForAdmin(email).subscribe({
      next: (u) => {
        this.identityPreview = { ...preview, userId: u.id };
        this.errorMessage = '';
        this.resolvingUserId = false;
      },
      error: () => {
        this.errorMessage = 'No user found for the email in this QR. Check spelling or use a numeric user id.';
        this.resolvingUserId = false;
      }
    });
  }

  private extractEmailFromRows(rows: { label: string; value: string }[]): string | null {
    const row = rows.find((r) => r.label.toLowerCase() === 'email');
    const v = row?.value?.trim();
    return v || null;
  }

  /** Parses CivicIdentity lines, legacy JSON, or a plain numeric id into a table-friendly structure. */
  private parseIdentity(raw: string): QrIdentityPreview | null {
    const trimmed = raw.trim();
    if (!trimmed) {
      return null;
    }

    if (/^\d+$/.test(trimmed)) {
      const id = parseInt(trimmed, 10);
      return {
        userId: Number.isFinite(id) ? id : null,
        rows: [{ label: 'User id', value: trimmed }]
      };
    }

    if (trimmed.startsWith('{')) {
      try {
        const o = JSON.parse(trimmed) as Record<string, unknown>;
        if (o == null || typeof o !== 'object') {
          return null;
        }
        const rows = Object.entries(o).map(([k, v]) => ({
          label: this.labelForKey(k),
          value: v === null || v === undefined ? '' : String(v)
        }));
        let userId: number | null = null;
        const v = o['userId'];
        if (typeof v === 'number' && Number.isFinite(v)) {
          userId = v;
        } else if (typeof v === 'string') {
          const n = parseInt(v, 10);
          userId = Number.isFinite(n) ? n : null;
        }
        return { userId, rows };
      } catch {
        return null;
      }
    }

    const lines = trimmed.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
    const rows: { label: string; value: string }[] = [];
    let userId: number | null = null;
    for (const line of lines) {
      const idx = line.indexOf(':');
      if (idx === -1) {
        rows.push({
          label: line.includes('CivicIdentity') ? 'Format' : 'Line',
          value: line
        });
        continue;
      }
      const key = line.slice(0, idx).trim();
      const value = line.slice(idx + 1).trim();
      if (key.toLowerCase() === 'userid' && /^\d+$/.test(value)) {
        userId = parseInt(value, 10);
      }
      rows.push({ label: this.labelForKey(key), value });
    }
    return rows.length > 0 ? { userId, rows } : null;
  }

  private labelForKey(key: string): string {
    const map: Record<string, string> = {
      userId: 'User id',
      userName: 'Username',
      userType: 'Role',
      badge: 'Badge',
      email: 'Email'
    };
    return map[key] ?? key;
  }
}
