create table if not exists media
(
    id            int generated by default as identity primary key,
    filename      varchar(255) not null,
    hash          varchar(64)  not null,
    creation_date timestamp(0)
);