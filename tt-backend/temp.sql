drop table track_names;
drop table highscore;
create table if not exists track_names(track integer primary key, name varchar(16));
create table if not exists highscore(track integer, pos integer, player varchar(16), length real, hard integer, taken integer, primary key (track, pos))

select * from highscore where pos = 1
select * from track_names;
delete from  track_names;
delete from highscore;

select track n, name, player pwner, pos from highscore inner join track_names using (track) where pos <= 3 order by track;
select player,count(*) cnt from (select player from highscore where pos = 1) group by player order by cnt desc, player;
select track,player,length,hard from highscore where pos<=50 order by track,length

select track n, name, player pwner, pos 
from highscore 
inner join track_names using (track) 
where (pos = 1 or player='[UWCS] Faux') and track in (select track from highscore where player='[UWCS] Faux')

select track n,name,pos,first,you,(you/first-1)*100 pace from (
select a.track,
(select length from highscore b where track=a.track and pos=1) first,
(select length from highscore b where track=a.track and player='[UWCS] Faux') you,
(select pos from highscore b where track=a.track and player='[UWCS] Faux') pos
from highscore a
group by a.track
) join track_names using (track)
where first is not null and you is not null
order by pace asc

select track n,name,pos,first,you,(you/first-1)*100 pace from (select a.track,(select length from highscore b where track=a.track and pos=1) first,(select length from highscore b where track=a.track and player=''[UWCS] Faux'') you, (select pos from highscore b where track=a.track and player=''[UWCS] Faux'') pos from highscore a group by a.track ) join track_names using (track) where first is not null and you is not null order by pace asc

select track n,name,(select pos from highscore b where player='[UWCS] Faux' and a.track=b.track) pos,
count(distinct length)-coalesce((select pos from highscore b where player='[UWCS] Faux' and a.track=b.track),0) cnt 
from highscore a inner join track_names using (track) group by track order by cnt desc limit 30

select distinct player from highscore
