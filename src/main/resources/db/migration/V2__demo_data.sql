-- Master data to exercise the endpoints without a separate employee API, which is not
-- part of iteration 1. The identifiers are fixed so they can be used in documentation
-- and integration tests.
insert into employer (id, name, created_at)
values ('11111111-1111-1111-1111-111111111111', 'Muster GmbH', now());

insert into employee (id, employer_id, personnel_number, first_name, last_name, external_employee_ref, active)
values ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'P-0001', 'Anna', 'Beispiel',
        'TT-1001', true),
       ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'P-0002', 'Bernd', 'Muster',
        'TT-1002', true),
       ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 'P-0003', 'Clara', 'Ehemalig',
        'TT-1003', false);

