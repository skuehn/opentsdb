package net.opentsdb.accumulo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.util.PeekingIterator;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.io.Text;
import org.hbase.async.DeleteRequest;
import org.hbase.async.GetRequest;
import org.hbase.async.KeyValue;
import org.hbase.async.PutRequest;

import com.stumbleupon.async.Deferred;


public class AccumuloClient {
	
	Connector conn;
  private MultiTableBatchWriter mtbw;
  short flushInterval = 1000;
	
	public AccumuloClient(Connector conn) {
		this.conn = conn;
		mtbw = this.conn.createMultiTableBatchWriter(100000, flushInterval, 4);
	}

	public Connector getConnector() {
		return conn;
	}

	public Deferred<Object> put(PutRequest put) {
	  Mutation m = new Mutation(new Text(put.key()));
	  m.put(new Text(put.family()), new Text(put.qualifier()), put.timestamp(), new Value(put.value()));
	  try {
	    synchronized (mtbw) {
	      mtbw.getBatchWriter(new String(put.table())).addMutation(m);
	    }
	    return Deferred.fromResult(null);
	  } catch (Exception ex) {
	    return Deferred.fromError(ex);
	  }
	}

	public Deferred<Object> delete(DeleteRequest del) {
	  Mutation m = new Mutation(new Text(del.key()));
	  for (byte[] qual : del.qualifiers()) {
	    m.putDelete(new Text(del.family()), new Text(qual));
	  }
    try {
      synchronized (mtbw) {
        mtbw.getBatchWriter(new String(del.table())).addMutation(m);
      }
      return Deferred.fromResult(null);
    } catch (Exception ex) {
      return Deferred.fromError(ex);
    }
	}

	public static KeyValue makeKeyValue(Entry<Key, Value> entry) {
		Key key = entry.getKey();
		return new KeyValue(TextUtil.getBytes(key.getRow()),
				TextUtil.getBytes(key.getColumnFamily()),
				TextUtil.getBytes(key.getColumnQualifier()),
				key.getTimestamp(),
				entry.getValue().get());
	}

	public Deferred<Object> flush() {
	  try {
	    synchronized (mtbw) {
	      mtbw.flush();
	    }
	    return Deferred.fromResult(null);
	  } catch (Exception ex) {
	    return Deferred.fromError(ex);
	  }
	}

	synchronized public void setFlushInterval(short time) {
	  try {
	    synchronized (mtbw) {
	      mtbw.close();
	    }
	  } catch (Exception ex) {
	    throw new RuntimeException(ex);
	  }
		mtbw = conn.createMultiTableBatchWriter(100000, flushInterval, 4);
	}

	synchronized public long getFlushInterval() {
		return flushInterval;
	}

	synchronized public Deferred<Object> shutdown() {
	  try {
	    synchronized (mtbw) {
	      mtbw.close();
	    }
	    mtbw = null;
	    return Deferred.fromResult(null);
	  } catch (Exception ex) {
	    return Deferred.fromError(ex);
	  }
	}

	public Deferred<ArrayList<KeyValue>> get(GetRequest get) {
	  try {
	    Scanner s = conn.createScanner(new String(get.table()), Constants.NO_AUTHS);
	    s.setRange(new Range(new Text(get.key())));
	    for (byte[] qual : get.qualifiers()) {
	      s.fetchColumn(new Text(get.family()), new Text(qual));
	    }
	    ArrayList<KeyValue> result = new ArrayList<KeyValue>();
	    for (Entry<Key,Value> e : s) {
	      result.add(makeKeyValue(e));
	    }
	    return Deferred.fromResult(result);
	  } catch (Exception ex) {
	    return Deferred.fromError(ex);
	  }
	}

	public Deferred<Object> ensureTableExists(String table) {
		if (conn.tableOperations().exists(table)) {
		  return Deferred.fromResult((Object)"exists");
		}
		return Deferred.fromError(new TableNotFoundException("?", table, "Table does not exist"));
	}

	public Scanner newScanner(byte[] table) {
	  try {
	    return conn.createScanner(new String(table), Constants.NO_AUTHS);
	  } catch (Exception ex) {
	    throw new RuntimeException(ex);
	  }
	}
	
	public static Iterable<ArrayList<KeyValue>> asRows(final Scanner scanner) {
	  return new Iterable<ArrayList<KeyValue>>() {
      public Iterator<ArrayList<KeyValue>> iterator() {
        final RowIterator riter = new RowIterator(new PeekingIterator<Entry<Key, Value>>(scanner.iterator()));
        
        return new Iterator<ArrayList<KeyValue>>() {
          public boolean hasNext() {
            return riter.hasNext();
          }

          public ArrayList<KeyValue> next() {
            ArrayList<KeyValue> result = new ArrayList<KeyValue>();
            Iterator<Entry<Key, Value>> row = riter.next();
            while (row.hasNext()) {
              result.add(makeKeyValue(row.next()));
            }
            return result;
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
	  };
	}
	
}
