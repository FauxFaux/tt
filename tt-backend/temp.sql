drop table track_names;
create table if not exists track_names(track integer primary key, name varchar(16));
create table if not exists highscore(track integer, pos integer, player varchar(16), length real, hard integer, primary key (track, pos))
select * from highscore where pos = 1
select * from track_names;
delete from  track_names;
delete from highscore;

select track n, name, player pwner from highscore inner join track_names using (track) where pos = 1 order by track;
select player,count(*) cnt from (select player from highscore where pos = 1) group by player order by cnt desc, player;
