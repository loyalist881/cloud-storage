--liquibase formatted sql

--changeset Pavel:5
insert into cloud.users (email, password)
values ('test@example.com',
        '$2a$12$iypGckxkEyxB.MKoNPsR/.FUvBw6Tvy3CgAmKkbpWQh7.bD1Y1dOW')
on conflict (email) do nothing;

