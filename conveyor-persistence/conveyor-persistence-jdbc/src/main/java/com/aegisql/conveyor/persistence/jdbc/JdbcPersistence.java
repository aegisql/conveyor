package com.aegisql.conveyor.persistence.jdbc;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
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
	
	private final ObjectConverter<K,String> converter;
	
	public JdbcPersistence(String driver, String connectionUrl, ObjectConverter<K,String> converter) throws ClassNotFoundException, SQLException {
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
	    this.converter = converter;
	    
	    Set<K> completed = getCompletedKeys();
	    archiveCompleteKeys(completed);
	    Collection oldCarts = getAllOldParts();
	    archiveAll();
	    for(Object cart:oldCarts) {
	    	long id = nextUniquePartId();
	    	savePart(id, (Cart) cart);
	    	savePartId(((Cart<K,?,?>)cart).getKey(), id);
	    }
	    
	}
	
	
	@Override
	public long nextUniquePartId() {
		return idSource.incrementAndGet();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO CARTS (ID,CART) VALUES (?,?)") ) {
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
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO CART_IDS (CART_KEY,CART_ID) VALUES (?,?)") ) {
	    	st.setString(1, converter.toPersistence(key));
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
	    try(PreparedStatement st = conn.prepareStatement("INSERT INTO COMPLETED (CART_KEY) VALUES (?)") ) {
	    	st.setString(1, converter.toPersistence(key));
	    	st.executeUpdate();
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
	    try(PreparedStatement st = conn.prepareStatement("SELECT CART FROM CARTS WHERE ID = ?") ) {
	    	st.setLong(1, id);
	    	ResultSet rs = st.executeQuery();
	    	Cart<K,?,L> cart = null;
	    	if(rs.next()){
	    		Blob blb = rs.getBlob(1);
	    		InputStream in = blb.getBinaryStream(1, blb.length());
	    		ObjectInputStream ois = new ObjectInputStream(in);
	    		cart = (Cart<K, ?, L>) ois.readObject();
	    	}
    		return cart;
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	@Override
	public Collection<Long> getAllPartIds(K key) {
	    try(PreparedStatement st = conn.prepareStatement("SELECT CART_ID FROM CART_IDS WHERE CART_KEY = ?") ) {
	    	st.setString(1, converter.toPersistence(key));
	    	ResultSet rs = st.executeQuery();
	    	Collection<Long> ids = new ArrayList<>();
	    	while(rs.next()){
	    		Long id = rs.getLong(1);
	    		ids.add(id);
	    	}
    		return ids;
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
	    try(PreparedStatement st = conn.prepareStatement("SELECT CART FROM CARTS WHERE ID > ?") ) {
	    	st.setLong(1,this.initialId);
	    	ResultSet rs = st.executeQuery();
	    	Collection<Cart<K,?,L>> carts = new ArrayList<>();
	    	while(rs.next()){
	    		Blob blb = rs.getBlob(1);
	    		InputStream in = blb.getBinaryStream(1, blb.length());
	    		ObjectInputStream ois = new ObjectInputStream(in);
	    		Cart<K,?,L> cart = (Cart<K, ?, L>) ois.readObject();
	    		carts.add(cart);
	    	}
    		return carts;
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	protected <L> Collection<Cart<K, ?, L>> getAllOldParts() {
	    try(PreparedStatement st = conn.prepareStatement("SELECT CART FROM CARTS") ) {
	    	ResultSet rs = st.executeQuery();
	    	Collection<Cart<K,?,L>> carts = new ArrayList<>();
	    	while(rs.next()){
	    		Blob blb = rs.getBlob(1);
	    		InputStream in = blb.getBinaryStream(1, blb.length());
	    		ObjectInputStream ois = new ObjectInputStream(in);
	    		Cart<K,?,L> cart = (Cart<K, ?, L>) ois.readObject();
	    		carts.add(cart);
	    	}
    		return carts;
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	
	@Override
	public Set<K> getCompletedKeys() {
	    try(PreparedStatement st = conn.prepareStatement("SELECT CART_KEY FROM COMPLETED") ) {
	    	ResultSet rs = st.executeQuery();
	    	Set<K> keys = new HashSet<>();
	    	while(rs.next()){
	    		String keyStr = rs.getString(1);
	    		keys.add( converter.fromPersistence(keyStr) );
	    	}
    		return keys;
	    } catch(Exception e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    	throw new RuntimeException("Save completed key failed",e);
	    }
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
	    try(PreparedStatement st = conn.prepareStatement("DELETE FROM CARTS WHERE ID = ?") ) {
	    	ids.forEach(id->{
	    		try {
					st.setLong(1, id);
			    	st.execute();	    		
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
	    	});
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
	    try(PreparedStatement st = conn.prepareStatement("DELETE FROM CART_IDS WHERE CART_KEY = ?") ) {
	    	keys.forEach(key->{
	    		try {
					st.setString(1, converter.toPersistence(key));
			    	st.execute();	    		
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
	    	});
	    } catch(SQLException e) {
	    	//e.printStackTrace();
	    	LOG.error("{}",e.getMessage());
	    }
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
	    try(PreparedStatement st = conn.prepareStatement("DELETE FROM COMPLETED WHERE CART_KEY = ?") ) {
	    	keys.forEach(key->{
	    		try {
					st.setString(1, converter.toPersistence(key));
			    	st.execute();	    		
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
	    	});
	    } catch(SQLException e) {
	    	LOG.error("{}",e.getMessage());
	    }
	    keys.forEach(key->{
	    	Collection<Long> completedIds = getAllPartIds(key);
	    	archiveParts(completedIds);
	    });
	    archiveKeys(keys);
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
