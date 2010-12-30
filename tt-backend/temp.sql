drop table track_names;
create table if not exists track_names(track integer primary key, name varchar(16));
create table if not exists highscore(track integer, pos integer, player varchar(16), length real, hard integer, primary key (track, pos))
select * from highscore where pos = 1
select * from track_names;
delete from  track_names;
