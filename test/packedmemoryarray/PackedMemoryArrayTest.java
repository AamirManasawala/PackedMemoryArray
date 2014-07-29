package packedmemoryarray;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import packedmemoryarray.PackedMemoryArray;

public class PackedMemoryArrayTest {
	@Test
	public void integrationTest() throws Exception {
		PackedMemoryArray  pmaOptimized = new  PackedMemoryArray();
		
		Random r = new Random();
		List<Integer> actualList = new ArrayList<Integer>();
		

		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < 10000000; i++) {
			int nextInt = r.nextInt();
			pmaOptimized.insert(nextInt);
			actualList.add(nextInt);
		}
		
		long endTime = System.currentTimeMillis();
		
		long timeElapsed = endTime - startTime;
		
		System.out.println("time elapsed: " + timeElapsed);
		
		System.out.println(pmaOptimized);
		List<Integer> pmaList = pmaOptimized.getList();
		Collections.sort(actualList);
		assertEquals(pmaList, actualList);

	}
	
}