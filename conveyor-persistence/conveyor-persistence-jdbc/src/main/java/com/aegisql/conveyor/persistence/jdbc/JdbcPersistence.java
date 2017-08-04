package com.aegisql.conveyor.persistence.jdbc;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

public class JdbcPersistence <K> implements Persistence<K> {

	private final static Logger LOG = LoggerFactory.getLogger(JdbcPersistence.class);
	
	private final Connection conn;
	private final long createdTimestamp = System.currentTimeMillis();
	//1501791526MMMXXXXXX 
	private final AtomicLong idSource = new AtomicLong( createdTimestamp * 1000000);
	private final long initialId = idSource.get();
	
	private final static String CARTS     = "CARTS";
	private final static String COMPLETED = "COMPLETED";
	
	public JdbcPersistence(String driver, String connectionUrl) throws ClassNotFoundException, SQLException {
		Class.forName(driver);
		conn = DriverManager.getConnection(connectionUrl);
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("CREATE TABLE CARTS (ID BIGINT PRIMARY KEY,CART BLOB)");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("CREATE TABLE COMPLETED (CART_KEY VARCHAR(10) PRIMARY KEY)");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("CREATE TABLE CART_IDS (CART_KEY VARCHAR(10),CART_ID BIGINT )");
	    	st.execute("CREATE INDEX CART_IDS_IDX ON CART_IDS(CART_KEY)");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }

	}
	
	
	@Override
	public long nextUniquePartId() {
		return idSource.incrementAndGet();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO TABLE CARTS (ID,CART) VALUES (?,?)") ) {
	    	Blob blob = conn.createBlob();
	    	OutputStream os = blob.setBinaryStream(1);
	    	ObjectOutputStream oos = new ObjectOutputStream(os);
	    	oos.writeObject(cart);
	    	st.setLong(1, id);
	    	st.setBlob(2, blob);
	    	st.executeUpdate();
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save Part failed",e);
	    }
	}

	@Override
	public void savePartId(K key, long partId) {
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO TABLE CART_IDS (CART_KEY,CART_ID) VALUES (?,?)") ) {
	    	st.setString(1, key.toString());
	    	st.setLong(2, partId);
	    	st.executeUpdate();
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save cart ID failed",e);
	    }
	}

	@Override
	public void saveCompletedBuildKey(K key) {
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO TABLE COMPLETED (CART_KEY) VALUES (?)") ) {
	    	st.setString(1, key.toString());
	    	st.executeUpdate();
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		return null;
	}

	@Override
	public Collection<Long> getAllPartIds(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<K> getCompletedKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveAll() {
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("DELETE FROM CARTS");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("DELETE FROM COMPLETED");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	    try(Statement st = conn.createStatement() ) {
	    	st.execute("DELETE FROM CART_IDS");
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	}

}
