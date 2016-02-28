package com.handy.ipcache.impl;

import java.net.InetAddress;
import java.util.ArrayList;
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

		public DoubleLinkedListNode(InetAddress address, Long lastUsedTime) {
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

	/**
	 * This operation consists add to hashmap O(1), and add to doubly linked
	 * list O(1) So time complexity of this method is O(1)
	 * 
	 */
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
					DoubleLinkedListNode newNode = new DoubleLinkedListNode(address, System.currentTimeMillis());
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

	/**
	 * O(1)
	 */
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

	/**
	 * This operation consists removing from hashmap O(1), and removing from
	 * doubly linked list O(1) So time complexity is O(1)
	 */
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

	/**
	 * O(1)
	 */
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

	/**
	 * O(1)
	 */
	@Override
	public InetAddress remove() {
		synchronized (cache) {
			if (!cache.isEmpty() && head != null) {
				DoubleLinkedListNode tmp = head;
				cache.remove(head.address.hashCode());
				removeNode(head);
				return tmp.address;
			} else {
				return null;
			}
		}

	}

	/**
	 * O(1)
	 */
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
	/**
	 * O(n)
	 */
	@Override
	public void close() {
		synchronized (cache) {
			cache.clear();
			this.head = null;
			this.end = null;
		}
	}
	/**
	 * O(1)
	 */
	@Override
	public int size() {
		synchronized (cache) {
			return cache.size();
		}
	}
	
	/**
	 * O(1)
	 */
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

	/**
	 * o(n)
	 */
	public void cleanup() {
		try {
			if (cachingTime > 0) {
				Thread.sleep(5000);
				ArrayList<Integer> deleteKey = new ArrayList<Integer>();
				synchronized (cache) {
					System.out.println(end);
					System.out.println(cache.keySet());
					for (int key : cache.keySet()) {
						if (cache.get(key).isExpired()) {
							deleteKey.add(key);
						}
					}
				}

				for (int key : deleteKey) {
					synchronized (cache) {
						DoubleLinkedListNode temp = cache.get(key);
						cache.remove(key);
						removeNode(temp);
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	{
		Thread cleaningThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (true) {
						cleanup();
						Thread.sleep(5000);
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
