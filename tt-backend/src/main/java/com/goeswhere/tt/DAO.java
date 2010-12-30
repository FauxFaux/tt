package com.goeswhere.tt;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DAO {
	private SimpleJdbcTemplate jdbc;

	@Autowired DAO(DataSource ds) {
		jdbc = new SimpleJdbcTemplate(ds);
	}

	String nameTrack(int id) {
		return jdbc.queryForObject("select name from track_names where id=?",
				String.class, id);
	}

	public void saveTrackNumber(int num, String name) {
		jdbc.update("insert into track_names (id, name) values (?,?)", num, name);
	}
}
