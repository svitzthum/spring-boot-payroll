-- The journal records what the external system sent, not what the domain accepts: an entry
-- is written precisely when the reported value could not be used. Mirroring the bounds of
-- WorkDuration here made that entry unwritable, so such an event was never recorded.
alter table time_tracking_import
    drop constraint ck_time_tracking_import_minutes;

