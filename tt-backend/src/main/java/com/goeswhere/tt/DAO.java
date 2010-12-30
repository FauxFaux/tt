package com.goeswhere.tt;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class DAO implements Closeable {
	private final Connection conn;
	private final Statement stat;

	public DAO(Connection conn) {
		this.conn = conn;
		try {
			this.stat = conn.createStatement();
		} catch (SQLException e) {
			throw new SQLRuntimeException(e);
		}
	}

	int executeUpdate(String sql, Object[] objects) {
		return executeUpdate(sql, ImmutableList.<Object[]>of(objects));
	}

	int executeUpdate(String sql, List<Object[]> args) {
		try {
			final PreparedStatement ps = conn.prepareStatement(sql);
			try {
				int ret = 0;
				for (Object[] arg : args) {
					prepare(ps, arg);
					ret += ps.executeUpdate();
				}
				return ret;
			} finally {
				ps.close();
			}
		} catch (SQLException e) {
			throw new SQLRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	<T> T lookup(String sql, Object[] args) {
		try {
			final PreparedStatement ps = conn.prepareStatement(sql);
			try {
				prepare(ps, args);
				final ResultSet rs = ps.executeQuery();
				try {
					if (!rs.next())
						throw new SQLRuntimeException("Expecting records to be returned");
					return (T)rs.getObject(1);
				} finally {
					rs.close();
				}
			} finally {
				ps.close();
			}
		} catch (SQLException e) {
			throw new SQLRuntimeException(e);
		}
	}

	private void prepare(final PreparedStatement ps, Object[] args) throws SQLException {
		for (int i = 0; i < args.length; i++)
			ps.setObject(i + 1, args[i]);
	}

	String nameTrack(int id) {
		return lookup("select name from track_names where track=?", new Object[] { id });
	}

	public void saveTrackTimes(int id, List<Object[]> args) {
		final TransactionManager tm = new TransactionManager();
		try {
			executeUpdate("delete from highscore where track=?", new Object[] { id });
			executeUpdate("insert into highscore (track, pos, player, length, hard)" +
					" values (?,?,?,?,?)", args);
			tm.commit();
		} finally {
			tm.close();
		}
	}


	public void saveTrackNumber(List<Object[]> l) {
		final TransactionManager tm = new TransactionManager();
		try {
			executeUpdate("insert or replace into track_names (track, name) values (?,?)", l);
			tm.commit();
		} finally {
			tm.close();
		}
	}

	private void execute(String command) {
		try {
			stat.execute(command);
		} catch (SQLException e) {
			throw new SQLRuntimeException(e);
		}
	}

	class TransactionManager implements Closeable {
		private boolean committed;

		TransactionManager() {
			begin();
		}

		public void commit() {
			DAO.this.commit();
			committed = true;
		}

		@Override
		public void close() {
			if (!committed)
				rollback();
		}
	}

	private void begin() {
		execute("begin");
	}

	private void commit() {
		execute("commit");
	}

	private void rollback() {
		execute("rollback");
	}

	@Override
	public void close() {
		try {
			stat.close();
			conn.close();
		} catch (SQLException e) {
			throw new SQLRuntimeException(e);
		}
	}
}
