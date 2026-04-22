# Tunisia Seed Report (TN26B1)

Date: 2026-04-22
Environment: `civic_platform` (MariaDB in `civic-platform-db-dev`)

## Update 2 - Business Rule Corrections + Enrichment

Applied after validation feedback:

- Campaign lifecycle rule enforced:
  - `COMPLETED` only when campaign progress is 100 percent
  - if not 100 percent and due date not passed -> `ACTIVE`
  - if not 100 percent and due date passed -> `CANCELLED`
- Project dataset normalized and verified present in DB.
- Added 20 additional active events.
- Updated 80 citizen profiles with diversified Tunisia-relevant details for more realistic matching/composite rates.

### Validation Results (Post-Fix)

- Campaigns (`[TN26B1-C%]`):
  - `ACTIVE`: 64
  - `COMPLETED`: 23
  - `CANCELLED`: 13
  - Invalid completed rows (completed without 100 percent): 0
  - Invalid active rows with past due date: 0
  - Invalid cancelled rows with future due date: 0

- Projects (`[TN26B1-P%]`):
  - Existing in DB: 100
  - Status distribution after normalization: `SUBMITTED` 100

- Events (`[TN26E1-%]`):
  - Added: 20
  - `UPCOMING`: 14
  - `ONGOING`: 6

- Citizen profile enrichment:
  - Citizens updated: 80
  - Citizens with non-empty address: 82
  - Citizens with non-empty interests: 82

## Scope

Bulk generation completed for:

- 100 campaigns
- 100 projects
- 100 posts

All records were generated with Tunisia-focused realistic content (real governorates/cities and civic impact themes), and created by users that have creator authorization in this project (`DONOR` or `AMBASSADOR`).

## Authorization Check (Creators Used)

Eligible creators (queried before generation):

- `id=6` - `croissantrouge.tn` - `DONOR`
- `id=106` - `match.amina.bensalah.0foodnexus.local` - `DONOR`
- `id=107` - `match.amina.bensalah.10foodnexus.local` - `AMBASSADOR`
- `id=109` - `match.hatem.trabelsi.7foodnexus.local` - `AMBASSADOR`
- `id=113` - `match.nour.bahri.6foodnexus.local` - `AMBASSADOR`
- `id=117` - `match.youssef.gharbi.4foodnexus.local` - `AMBASSADOR`

## Naming / Batch Tag

Generated rows are tagged for traceability:

- Campaigns: `name LIKE '[TN26B1-C%'`
- Projects: `title LIKE '[TN26B1-P%'`
- Posts: `content LIKE '[TN26B1-S%'`

## Result Counts

- `campaigns_seeded = 100`
- `projects_seeded = 100`
- `posts_seeded = 100`

## Creator Distribution

### Campaigns (100)

- `id=109` -> 20
- `id=117` -> 19
- `id=106` -> 17
- `id=113` -> 17
- `id=6` -> 14
- `id=107` -> 13

### Projects (100)

- `id=107` -> 24
- `id=109` -> 23
- `id=113` -> 16
- `id=117` -> 15
- `id=106` -> 12
- `id=6` -> 10

### Posts (100)

- `match.amina.bensalah.10foodnexus.local` -> 24
- `match.hatem.trabelsi.7foodnexus.local` -> 20
- `match.youssef.gharbi.4foodnexus.local` -> 17
- `match.nour.bahri.6foodnexus.local` -> 14
- `match.amina.bensalah.0foodnexus.local` -> 14
- `croissantrouge.tn` -> 11

## Post-Campaign Linking

For realism in content flows:

- `26` posts of type `CAMPAIGN_ANNOUNCEMENT` were linked to generated campaigns (`campaign_id` assigned).

## Tunisia Data Characteristics Used

The generated records include real Tunisia context:

- Cities/governorates such as `Tunis`, `Sfax`, `Sousse`, `Nabeul`, `Gabes`, `Kairouan`, `Bizerte`, `Gafsa`, `Tozeur`, `Ariana`, `Ben Arous`, `Manouba`, `Zaghouan`, `Kebili`, `Jendouba`, `Le Kef`, etc.
- Civic-impact themes such as:
  - food waste reduction
  - community fridge restock
  - school meal solidarity
  - Ramadan iftar support
  - women artisan empowerment
  - coastal cleanup
  - rural water access
  - digital skills for youth
  - medical caravan logistics

## Sample Rows

### Campaign samples

- `[TN26B1-C100] Digital Skills for Youth - Gafsa`
- `[TN26B1-C099] School Meal Solidarity - Jendouba`
- `[TN26B1-C098] Coastal Cleanup - Siliana`

### Project samples

- `[TN26B1-P100] Digital Skills for Youth Infrastructure - Zaghouan`
- `[TN26B1-P099] Digital Skills for Youth Infrastructure - Gabes`
- `[TN26B1-P098] Medical Caravan Logistics Infrastructure - Sousse`

### Post samples

- `[TN26B1-S100] Field update from Ben Arous: Fishermen Safety Training ...`
- `[TN26B1-S099] Field update from Kairouan: Digital Skills for Youth ...`
- `[TN26B1-S098] Field update from Siliana: Community Fridge Restock ...`

