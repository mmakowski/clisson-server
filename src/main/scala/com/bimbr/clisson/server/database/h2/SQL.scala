package com.bimbr.clisson.server.database.h2

/**
 * All SQL statements executed against H2 database.
 * 
 * @author mmakowski
 * @since 1.0.0
 */
private[h2] object SQL {
  import EventTypes._ 
  import MessageRoles._ 
 
  val InitialisedCheck = "select * from metadata;"
  
  val InitDdl = """

-- event types
create table event_types (
  type_id   TINYINT     not null,
  type_name VARCHAR(16) not null,
  -- constraints
  primary key (type_id)
);

-- events
create sequence event_id_seq;
create table events (
  event_id    BIGINT      not null,
  source      VARCHAR(64) not null,
  timestamp   TIMESTAMP   not null,
  priority    INT         not null,
  type        TINYINT     not null,
  description VARCHAR,
  -- constraints
  primary key (event_id),
  foreign key (type) references event_types (type_id) on delete cascade
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
  primary key (role_id)
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
insert into event_types (type_id, type_name) values (""" + Checkpoint + """, '""" + typeName(Checkpoint) + """');
insert into event_types (type_id, type_name) values (""" + Split + """, '""" + typeName(Split) + """');
insert into event_types (type_id, type_name) values (""" + Join + """, '""" + typeName(Join) + """');
insert into message_roles (role_id, description) values (""" + CheckpointMsg + """, 'a message that passed through the checkpoint');
insert into message_roles (role_id, description) values (""" + SourceMsg + """, 'a source message of a transformation');
insert into message_roles (role_id, description) values (""" + ResultMsg + """, 'a result message of a transformation');
insert into metadata (key, value) values ('schema.version', '0.1.0');
"""
  val SelectNextEventId = """select nextval('event_id_seq') from dual;""" 

  val InsertEvent = """insert into events (event_id, source, timestamp, priority, type, description) values (?, ?, ?, ?, ?, ?);"""
  
  val SelectNextMessageId = """select nextval('message_id_seq') from dual;"""
    
  val SelectMessageId = """select message_id from message_ids where external_id = ?;"""
    
  val InsertMessageId = """insert into message_ids (external_id, message_id) values (?, ?);"""
    
  val InsertEventMessage = """insert into event_messages (event_id, message_id, message_role) values (?, ?, ?);"""
    
  // FIXME: this will be a mess once we have more than 1 external id
  val SelectEventsForExternalId = """
select e.event_id
     , e.source
     , e.timestamp
     , e.priority
     , e.type
     , e.description
     , m.external_id
     , em.message_role
from message_ids msrc
   , event_messages emsrc
   , events e
   , event_messages em
   , message_ids m
where msrc.external_id = ?
  and msrc.message_id  = emsrc.message_id
  and emsrc.event_id   = e.event_id
  and e.event_id       = em.event_id
  and em.message_id    = m.message_id  
"""
}