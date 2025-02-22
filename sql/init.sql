create database board;
\c board;

CREATE TABLE jobs
(
    id uuid DEFAULT gen_random_uuid(),
    date bigint NOT NULL ,
    ownerEmail text NOT NULL ,
    company text NOT NULL ,
    title text NOT NULL ,
    description text NOT NULL ,
    externalUrl text NOT NULL ,
    remote boolean NOT NULL default false,
    location text NOT NULL ,
    salaryLo integer,
    salaryHigh integer,
    currency text,
    country text,
    tags text[],
    image text,
    seniority text,
    other text,
    active boolean NOT NULL default FALSE
);

ALTER TABLE jobs
add constraint pk_jobs primary key (id);