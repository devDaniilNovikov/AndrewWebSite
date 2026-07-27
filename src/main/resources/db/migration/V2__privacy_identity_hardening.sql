update leads
set source_path = '/'
where anonymized_at is not null
  and source_path <> '/';

alter table leads
    drop constraint ck_leads_privacy;

alter table leads
    add constraint ck_leads_privacy check (
        (
            anonymized_at is null
            and payload_fingerprint is not null
            and name is not null
            and phone is not null
        )
        or
        (
            anonymized_at is not null
            and payload_fingerprint is null
            and name is null
            and phone is null
            and comment is null
            and source_path = '/'
        )
    );

alter table leads
    add constraint ck_leads_request_id_v4 check (
        anonymized_at is not null
        or uuid_extract_version(request_id) is not distinct from 4
    ) not valid;
