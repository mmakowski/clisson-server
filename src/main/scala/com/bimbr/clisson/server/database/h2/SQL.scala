package com.bimbr.clisson.server.database.h2

private[h2] object SQL {
  import Constants._ 
 
  val InitialisedCheck = "select * from metadata;"
  
  val InitDdl = """
    
-- events
create sequence event_id_seq;
create table events (
  event_id  BIGINT    not null,
  timestamp TIMESTAMP not null,
  priority  INT       not null,
  description VARCHAR,
  -- constraints
  primary key (event_id)
);

-- messages
create sequence message_id_seq;
create table message_ids (
  external_id VARCHAR not null,
  message_id  BIGINT  not null,
  -- constraints
  primary key (external_id)
);

-- message roles in events
create table message_roles (
  role_id     TINYINT     not null,
  description VARCHAR(64) not null,
  -- constraints
  primary key (role_id),
);

-- message ids corresponding to events
create table event_messages (
  event_id     BIGINT  not null,
  message_id   BIGINT  not null,
  message_role TINYINT not null,
  -- constratints
  primary key (event_id, message_id),
  foreign key (event_id) references events (event_id) on delete cascade,
  foreign key (message_id) references message_ids (message_id) on delete cascade,
  foreign key (message_role) references message_roles (role_id) on delete cascade 
);
    
-- metadata
create table metadata (
  key   varchar(32)  not null,
  value varchar(128) not null
);
"""
  
  val InitDml = """
-- message roles
insert into message_roles (role_id, description) values (""" + CheckpointMsg + """, 'the message that passed through the checkpoint');
insert into message_roles (role_id, description) values (""" + SourceMsg + """, 'the source message of a transformation');
insert into message_roles (role_id, description) values (""" + ResultMsg + """, 'the result message of a transformation');
insert into metadata (key, value) values ('schema.version', '0.1.0');
"""
  val SelectNextEventId = """select nextval('event_id_seq') from dual;""" 

  val InsertEvent = """insert into events (event_id, timestamp, priority, description) values (?, ?, ?, ?);"""
  
  val SelectNextMessageId = """select nextval('message_id_seq') from dual;"""
    
  val SelectMessageId = """select message_id from message_ids where external_id = ?;"""
    
  val InsertMessageId = """insert into message_ids (external_id, message_id) values (?, ?);"""
    
  val InsertEventMessage = """insert into event_messages (event_id, message_id, message_role) values (?, ?, ?);"""
}