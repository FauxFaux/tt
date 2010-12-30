package com.goeswhere.tt;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DAO {
	private SimpleJdbcTemplate jdbc;

	@Autowired public DAO(DataSource ds) {
		jdbc = new SimpleJdbcTemplate(ds);
	}

	String nameTrack(int id) {
		return jdbc.queryForObject("select name from track_names where track=?",
				String.class, id);
	}

	private static final String INSERT = "insert or replace into track_names (track, name) values (?,?)";
	public void saveTrackNumber(int num, String name) {
		jdbc.update(INSERT, num, name);
	}

	public void saveTrackNumber(List<Object[]> l) {
		jdbc.batchUpdate(INSERT, l);
	}

	@Transactional
	public void saveTrackTimes(int id, List<Object[]> args) {
		jdbc.update("delete from highscore where track=?", id);
		jdbc.batchUpdate("insert into highscore (track, pos, player, length, hard)" +
				" values (?,?,?,?,?)", args);
	}
}
