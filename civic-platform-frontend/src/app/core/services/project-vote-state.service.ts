import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Shared vote state for projects so list + detail stay in sync with the server
 * (one vote per user per project, stored in DB).
 */
@Injectable({ providedIn: 'root' })
export class ProjectVoteStateService {
  private readonly state$ = new BehaviorSubject<Map<number, boolean>>(new Map());

  /** Emits whenever known vote flags change (from API or after a successful vote). */
  readonly changes = this.state$.asObservable();

  snapshot(): Map<number, boolean> {
    return new Map(this.state$.value);
  }

  /** True if we have loaded / set the user's vote for this project. */
  hasLoaded(projectId: number): boolean {
    return this.state$.value.has(projectId);
  }

  isVoted(projectId: number): boolean {
    return !!this.state$.value.get(projectId);
  }

  setFromServer(projectId: number, voted: boolean): void {
    const m = new Map(this.state$.value);
    m.set(projectId, voted);
    this.state$.next(m);
  }

  mergeFromServer(rows: { projectId: number; voted: boolean }[]): void {
    if (rows.length === 0) {
      return;
    }
    const m = new Map(this.state$.value);
    for (const r of rows) {
      m.set(r.projectId, r.voted);
    }
    this.state$.next(m);
  }

  /** Call after a successful POST vote (list or detail). */
  markVoted(projectId: number): void {
    const m = new Map(this.state$.value);
    m.set(projectId, true);
    this.state$.next(m);
  }

  /** Clear all known flags (e.g. logout). */
  clear(): void {
    this.state$.next(new Map());
  }
}
