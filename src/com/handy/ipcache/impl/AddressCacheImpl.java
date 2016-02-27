package com.handy.ipcache.impl;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.handy.ipcache.AddressCache;

/**
 * This class implements {@link AddressCache}.
 * 
 * @author Henry Pan
 *
 */
public class AddressCacheImpl implements AddressCache {
	private static int CACHE_SIZE_DEFAULT = 10;
	private long cachingTime;
	private int capacity;
	private HashMap<Integer, DoubleLinkedListNode> cache;
	private DoubleLinkedListNode head;
	private DoubleLinkedListNode end;
	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	protected class DoubleLinkedListNode {
		public int key;
		public InetAddress address;
		public Long lastUsedTime;

		public DoubleLinkedListNode prev;
		public DoubleLinkedListNode next;

		public DoubleLinkedListNode(int key, InetAddress address, Long lastUsedTime) {
			this.key = key;
			this.address = address;
			this.lastUsedTime = lastUsedTime;

		}

		public boolean isExpired() {
			return (lastUsedTime + cachingTime) < System.currentTimeMillis();
		}
	}

	/**
	 * A hash map is used to implement the cache, key is the hash code of the IP
	 * instance, value is {@link InetAddress} instance
	 */
	public AddressCacheImpl(int capacity, Long cachingTime) {
		this.capacity = capacity;
		this.cache = new HashMap<Integer, DoubleLinkedListNode>(capacity);
		this.cachingTime = cachingTime;
	}

	/**
	 * Set the default cache size and caching time if not provided.
	 */
	public AddressCacheImpl() {
		this(CACHE_SIZE_DEFAULT, 0L);
	}

	@Override
	public boolean offer(InetAddress address) {
		lock.lock();
		boolean isOffered;
		try {
			int key = address.hashCode();
			if (cache.containsKey(key)) {
				DoubleLinkedListNode oldNode = cache.get(key);
				oldNode.address = address;
				removeNode(oldNode);
				setHead(oldNode);
				isOffered = true;
			} else {
				if (cache.size() >= capacity) {
					isOffered = false;
				} else {
					DoubleLinkedListNode newNode = new DoubleLinkedListNode(key, address, System.currentTimeMillis());
					setHead(newNode);
					cache.put(key, newNode);
					isOffered = true;
				}
			}
			if (isOffered) {
				notEmpty.signal();
			}
			return isOffered;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean contains(InetAddress address) {
		synchronized (cache) {
			if (cache.containsKey(address.hashCode())) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean remove(InetAddress address) {
		synchronized (cache) {
			int key = address.hashCode();
			if (cache.containsKey(key)) {
				DoubleLinkedListNode nodeToDelete = cache.get(key);
				cache.remove(key);
				removeNode(nodeToDelete);
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public InetAddress peek() {
		synchronized (cache) {
			if (!cache.isEmpty() && head != null) {
				return head.address;
			} else {
				return null;
			}
		}

	}

	@Override
	public InetAddress remove() {
		synchronized (cache) {
			if (!cache.isEmpty() && head != null) {
				DoubleLinkedListNode tmp = head;
				cache.remove(head.key);
				removeNode(head);
				return tmp.address;
			} else {
				return null;
			}
		}

	}

	@Override
	public InetAddress take() throws InterruptedException {
		lock.lock();
		try {
			while (cache.isEmpty()) {
				notEmpty.await();
			}
			DoubleLinkedListNode tmp = head;
			cache.remove(head.key);
			removeNode(head);
			return tmp.address;
		} finally {
			lock.unlock();
		}

	}

	@Override
	public void close() {
		synchronized (cache) {
			cache.clear();
			this.head = null;
			this.end = null;
		}
	}

	@Override
	public int size() {
		synchronized (cache) {
			return cache.size();
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (cache) {
			return head == null;
		}

	}

	public long getCachingTime() {
		return cachingTime;
	}

	public void setCachingTime(long cachingTime) {
		this.cachingTime = cachingTime;
	}

	private void removeNode(DoubleLinkedListNode node) {
		DoubleLinkedListNode current = node;
		DoubleLinkedListNode prev = current.prev;
		DoubleLinkedListNode next = current.next;

		if (prev != null) {
			prev.next = next;
		} else {
			head = next;
		}

		if (next != null) {
			next.prev = prev;
		} else {
			end = prev;
		}
	}

	private void setHead(DoubleLinkedListNode node) {
		node.next = head;
		node.prev = null;
		if (head != null) {
			head.prev = node;
		}
		head = node;
		if (end == null) {
			end = head;
		}
	}

	{
		Thread cleaningThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (cachingTime > 0) {
						while (true) {
							if (!cache.isEmpty() && end != null) {
								if (cache.get(end.key).isExpired()) {
									cache.remove(end.key);
									removeNode(end);
								}
							}
							Thread.sleep(5000);
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		cleaningThread.setDaemon(true);
		cleaningThread.start();

	}
}
