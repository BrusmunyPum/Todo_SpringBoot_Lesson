create table task_comments (
    id bigserial primary key,
    task_id bigint not null ,
    content varchar(500) not null ,
    created_at timestamp with time zone not null ,
    updated_at timestamp with time zone not null ,

    constraint fk_task_comments_task foreign key (task_id) references tasks(id)
);