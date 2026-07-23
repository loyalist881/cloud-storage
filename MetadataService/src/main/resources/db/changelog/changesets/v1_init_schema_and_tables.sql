--liquibase formatted sql

--changeset Pavel:1
create schema if not exists cloud;

--changeset Pavel:2
create table if not exists cloud.users
(
    id       bigserial primary key,
    email    varchar(255) not null unique,
    password varchar(255) not null
);

--changeset Pavel:3
create table if not exists cloud.metadata
(
    id           bigserial primary key,
    user_id      bigint       not null,
    filename     varchar(255) not null,
    s3_key       varchar(500)  not null,
    size_file    bigint       not null,
    content_type varchar(100)
);

--changeset Pavel:4
alter table cloud.metadata
    add constraint fk_metadata_user
    foreign key (user_id)
    references cloud.users (id)
    on delete cascade;