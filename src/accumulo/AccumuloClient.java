package net.opentsdb.accumulo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.WholeRowIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.io.Text;
import org.hbase.async.AtomicIncrementRequest;
import org.hbase.async.DeleteRequest;
import org.hbase.async.GetRequest;
import org.hbase.async.KeyValue;
import org.hbase.async.PutRequest;
import org.hbase.async.RowLock;
import org.hbase.async.RowLockRequest;

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
		m.put(new Text(put.family()), new Text(put.qualifier()),
				put.timestamp(), new Value(put.value()));
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
				TextUtil.getBytes(key.getColumnFamily()), TextUtil.getBytes(key
						.getColumnQualifier()), key.getTimestamp(), entry
						.getValue().get());
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
			Scanner s = conn.createScanner(new String(get.table()),
					Constants.NO_AUTHS);
			s.setRange(new Range(new Text(get.key())));
			for (byte[] qual : get.qualifiers()) {
				s.fetchColumn(new Text(get.family()), new Text(qual));
			}
			ArrayList<KeyValue> result = new ArrayList<KeyValue>();
			for (Entry<Key, Value> e : s) {
				result.add(makeKeyValue(e));
			}
			return Deferred.fromResult(result);
		} catch (Exception ex) {
			return Deferred.fromError(ex);
		}
	}

	public Deferred<Object> ensureTableExists(String table) {
		if (conn.tableOperations().exists(table)) {
			return Deferred.fromResult((Object) "exists");
		}
		return Deferred.fromError(new TableNotFoundException("?", table,
				"Table does not exist"));
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

				try {
					scanner.setScanIterators(Integer.MAX_VALUE,
							WholeRowIterator.class.getName(),
							"asRows wholerow iterator");
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
				final Iterator<Entry<Key, Value>> scanIter = scanner.iterator();

				return new Iterator<ArrayList<KeyValue>>() {

					public boolean hasNext() {
						return scanIter.hasNext();
					}

					public ArrayList<KeyValue> next() {
						ArrayList<KeyValue> kvList = new ArrayList<KeyValue>();
						Entry<Key, Value> row = scanIter.next();
						SortedMap<Key, Value> wholeRow;
						try {
							wholeRow = WholeRowIterator.decodeRow(row.getKey(),
									row.getValue());
						} catch (IOException ioe) {
							throw new RuntimeException(ioe);
						}
						for (Entry<Key, Value> cell : wholeRow.entrySet()) {
							kvList.add(makeKeyValue(cell));
						}
						return kvList;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public Object lockRow(RowLockRequest anyRowLockRequest) {
		throw new UnsupportedOperationException();
	}

	public void unlockRow(RowLock fake_lock) {
		throw new UnsupportedOperationException();
	}

	public void atomicIncrement(AtomicIncrementRequest any) {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] argv) throws Exception {

		ZooKeeperInstance zk = new ZooKeeperInstance("skuehn-test-1.3.5", "localhost");
		Connector c = zk.getConnector("root", "cat".getBytes());

		Scanner scanner = c.createScanner("cellscantest", new Authorizations());

		Iterable<ArrayList<KeyValue>> rows = AccumuloClient.asRows(scanner);
		int rowIdx = 0;
		for (ArrayList<KeyValue> row : rows) {
			System.err.println("Row [" + rowIdx++ + "] num cells: "
					+ row.size());
			for (KeyValue cell : row) {
				System.err.println("     k: " + new String(cell.key()) + " v: "
						+ new String(cell.value()));
			}
		}
	}
}
