# Equipment, completed-work, and team images

## Objective

Replace matching equipment, completed-work, and team placeholders with photographs
supplied and assigned by the user, while preserving the static-export,
accessibility, and verified-local-media contracts. Keep equipment cards concise
by removing the duplicated expandable symptom and work-example sections.

## Ownership and scope

- **Owner:** Codex under the user's 2026-09-13 through 2026-09-18 authorizations.
- **Base:** fresh `origin/main` at `4dd990d`.
- **Owned paths:** assigned media under `frontend/public/media/verified/`,
  equipment, completed-work, and team content and rendering, directly related frontend
  tests, standalone-export handling needed for those same photos, this task
  brief, the preview contract, tracker metadata, and the task handoff.
- **Out of scope:** all other placeholders, unsupported business copy, backend
  behavior, and deployment. The latest user request clarifies that the commit
  must appear in Timeweb's `main` commit picker. This requires a published PR
  and merge under the repository Git Flow; Ready still requires its separate
  authorization and green required checks.

## Plan

1. Record the supplied files, exact card assignments, and user-confirmed case
   copy.
2. Optimize and render the photos with descriptive alternatives and responsive
   sizing through the existing `MediaSlot` contract. Fit every supplied photo
   inside its frame without cropping or changing its aspect ratio, and remove
   the duplicated equipment-card detail expander.
3. Run focused tests, frontend quality gates, export checks, and diff review.

## Supplied media

| Card                    | Source SHA-256                                                     | Published asset                       |
| ----------------------- | ------------------------------------------------------------------ | ------------------------------------- |
| Холодильные камеры      | `e38d3a74b3155cc8f3f92a52c7a0b79220af0ab14c42b081e892a87c7e4a73b4` | `equipment-cold-rooms.jpg`            |
| Витрины и горки         | `ca3e759ff38bf184e987ad8f07bfc355eef970a6d57c0672dffc591688346642` | `equipment-display-cases.webp`        |
| Льдогенераторы          | `98990fed7576921252a1723a5ad062e85364974980f82c329b774afa78dba82d` | `equipment-ice-makers.webp`           |
| Морозильные лари        | `6eabf2d59ad7701dbc813a105b7a5170bfc10afd4e5a0e1550a2c12da6e88816` | `equipment-chest-freezers.jpg`        |
| Холодильные системы     | `a510a558331e35f5ea1e1ade6d3c857d8eee1ff1099ce861ce3d56e4d664c0ce` | `equipment-refrigeration-systems.jpg` |
| Шкафы и столы           | `3abf7f6a1285db556ba5162ee597c975085c864347b4bb11a0040c86c086450d` | `equipment-cabinets-and-tables.jpg`   |
| Кейс ледогенератора     | `35760910f153802132650210946a53b8f2a66f30f24bbaeb2a94a68d72f991ff` | `work-ice-maker.jpg`                  |
| Кейс холодильного шкафа | `6ef7bf43fc32c230f5e55fb3f17d0b49a5a30899e57ef2bbb33bae3d3fe7eb84` | `work-refrigerated-cabinet.jpg`       |
| Кейс торгового объекта  | `8f51057f8fb946c18e8de14befa742c56b4c37c8b9b7ef3026806b9ed42f65f7` | `work-retail-site.jpg`                |
| Фото команды            | `6dd7966926815984411ff00b49fd2535ce8dd5c570b27d1f3734113a8d0eed7a` | `about-team-installation.webp`        |

## User-confirmed case copy

### Ice maker

- **Title:** Переход ледогенератора на воздушное охлаждение.
- **Problem:** Чрезмерное потребление воды и утечка фреона.
- **Result:** Ледогенератор стабильно работает и меньше потребляет воды.

### Industrial refrigeration unit

- **Title:** Поиск утечки промышленного агрегата.
- **Problem:** Утечка фреона в централи холодильной системы.
- **Result:** Система была опрессована, была найдена утечка в нескольких местах
  и была устранена.

### Evaporator restoration

- **Title:** Реставрация испарителя холодильного шкафа/стола.
- **Problem:** Утечка фреона в системе.
- **Result:** Реставрация испарителя путем замены трубок и уголков. Утечки нет,
  пожизненная гарантия.

On 2026-09-20 the user reconfirmed rights to publish all supplied photographs
and consent from the people shown in the team portrait. This confirmation
applies to the ten assets listed above; new media needs a fresh confirmation.
The user's placement assignment is recorded; no customer, certification,
case-result, or brand-relationship claim is inferred from the photographs.
