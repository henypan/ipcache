package com.handy.ipcache.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.handy.ipcache.AddressCache;

public class AddressCacheImplTest {

	public AddressCache addressCache;
	public InetAddress ip1;
	public InetAddress ip2;
	public InetAddress ip3;

	@Before
	public void setUp() throws Exception {
		addressCache = new AddressCacheImpl();
		ip1 = InetAddress.getByName("10.107.1.1");
		ip2 = InetAddress.getByName("10.107.1.2");
		ip3 = InetAddress.getByName("10.107.1.3");
	}

	@After
	public void tearDown() throws Exception {
		addressCache.close();
	}

	@Test
	public void testOffer() {

		addressCache.offer(ip1);
		addressCache.offer(ip2);
		addressCache.offer(ip3);
		addressCache.offer(ip2);
		assertEquals(ip2, addressCache.remove());
		assertEquals(ip3, addressCache.remove());
		assertEquals(ip1, addressCache.remove());
		assertTrue(addressCache.remove() == null);
	}

	@Test
	public void testContains() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		assertTrue(addressCache.contains(ip1));
	}

	@Test
	public void testPeek() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		assertEquals(ip2, addressCache.peek());
		assertEquals(ip2, addressCache.peek());
	}

	@Test
	public void testRemoveLatest() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		assertEquals(ip2, addressCache.remove());
		assertEquals(ip1, addressCache.peek());
	}

	@Test
	public void testRemove() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		assertEquals(true, addressCache.remove(ip1));
		assertEquals(ip2, addressCache.peek());
	}

	@Test
	public void testTake() throws InterruptedException {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					addressCache.offer(ip1);
					Thread.sleep(2000);
					addressCache.offer(ip2);
					Thread.sleep(2000);
					addressCache.offer(ip3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).run();

		
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					for (int i = 0; i < 3; i++) {
						assertTrue(Arrays.asList(ip1, ip2, ip3).contains(addressCache.take()));
						Thread.sleep(1000);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).run();
	}

	@Test
	public void testClose() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		addressCache.close();
		assertTrue(addressCache.isEmpty());
	}

	@Test
	public void testSize() {
		addressCache.offer(ip1);
		addressCache.offer(ip2);
		addressCache.offer(ip3);
		assertEquals(3, addressCache.size());
		
		addressCache.remove();
		addressCache.remove();
		assertEquals(1, addressCache.size());
		
		addressCache.remove();
		assertEquals(0, addressCache.size());
	}

	@Test
	public void testIsEmpty() {
		assertTrue(addressCache.isEmpty());
	}
}
