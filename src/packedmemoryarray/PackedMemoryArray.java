package packedmemoryarray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackedMemoryArray {
	
	private static final double HIGHEST_UPPER_DENSITY = 1.0d;
	private static final double LOWEST_UPPER_DENSITY = .5d;
	private final int BASE_CASE = 64;
	
	public int actualCapacity; 
	public int chunkSize;
	public int numberOfChunks;
	public int numberOfLevels;
	private int numberOfElemenetsInserted;
	
	private List<Integer> baseCaseStorage = new ArrayList<Integer>();

	private int[] physicalStorage;
	private boolean[] flag; 
	
	public PackedMemoryArray() {
	}
	
	public void setParameters (int capacity) {
		this.actualCapacity = capacity;
		this.chunkSize = powerOfTwo(logBaseTwo(logBaseTwo(capacity) * 2));
		this.numberOfChunks = actualCapacity / chunkSize;
		this.numberOfLevels = logBaseTwo(this.numberOfChunks);
	}
		
	public void insert(int element) {
		
		if( numberOfElemenetsInserted > BASE_CASE - 1) {
			insertPMA(element);
		}
		
		else if(numberOfElemenetsInserted < BASE_CASE -1) {
			baseCaseStorage.add(element);
			numberOfElemenetsInserted ++;
		}	
		else {
			baseCaseStorage.add(element);
			Collections.sort(baseCaseStorage);
			physicalStorage = new int[BASE_CASE * 4];
			flag = new boolean[BASE_CASE*4];
			setParameters(BASE_CASE * 4);
			int numberOfElementsPerChunk = BASE_CASE / numberOfChunks;
			int baseIndex = 0;
			for(int i = 0; i < numberOfChunks; i++) {
				for(int j = 0; j < numberOfElementsPerChunk ; j++) {
					physicalStorage[i*chunkSize + j] = baseCaseStorage.get(baseIndex);
					flag[i*chunkSize + j] = true;
					baseIndex++;
				}
			}
			numberOfElemenetsInserted++;
		}
	}

	private void insertPMA (int element) {
		int chunkNumber = chunkForInsertion(element);
		PMAStatistics stats = insertAtLowestChunk(element, chunkNumber);
		if(stats.inLimit)
			return;
		
		int physicalElements = stats.numberOfElements ;
		int counterChild = getCounterChild(0, chunkNumber);
		
		for(int level = 1;level <= numberOfLevels;level++) {
			PMAStatistics levelStats =  statistics(level, chunkNumber, counterChild, physicalElements);
			if(levelStats.inLimit) {
				insertAtLevel(level, chunkNumber, element);
				return;
			}
			counterChild = getCounterChild(level, chunkNumber);
			// this is to avoid recounting on one side of the tree. 
			physicalElements = levelStats.numberOfElements; 
		}
		resize(element);
	}
	

	private PMAStatistics statistics(int level, int chunkNumber, int child, int elements) {
		
		int chunkLength = powerOfTwo(level) * this.chunkSize;
		int chunkNumberAtLevel = (chunkNumber * this.chunkSize)/chunkLength ;

		int startIndex = chunkNumberAtLevel * chunkLength ;
		
		if(child == 0) {
			// left child needs to be computed, 
			startIndex = chunkNumberAtLevel * chunkLength;
		}
		else {
			startIndex = chunkNumberAtLevel * chunkLength + (chunkLength/2);
		}
		
		int endIndex = startIndex + (chunkLength/2);
		
		//System.out.println("start index: " + startIndex);
		//System.out.println("end index: " + endIndex);
		
		int totalElementsPresent = 0;
		for(int i = startIndex; i < endIndex; i++) {
			if(flag[i]) {
				totalElementsPresent++;
			}
		}
	
		totalElementsPresent += elements;
		double physicalThreshold = (double)totalElementsPresent/(double)(chunkLength);
		return new PMAStatistics (physicalThreshold <= upperThresholdAt(level), totalElementsPresent);
	}

	
	public int getCounterChild (int level, int chunkNumber) {
		int chunkLength = powerOfTwo(level) * this.chunkSize;
		int chunkNumberAtLevel = (chunkNumber * this.chunkSize)/chunkLength ;
		if(chunkNumberAtLevel % 2 == 0) {
			return 1;
		}
		// i am the right child, return its counter which is left
		return 0;
	}
	
	public boolean veriryIsSorted() {
		int previous = Integer.MIN_VALUE;
		for (int i = 0; i < physicalStorage.length ; i++) {
			if (flag[i]) {
				if(previous > physicalStorage[i])
					return false;
				previous = physicalStorage[i];
			}
		}
		return true;
	}
	
	private void resize(int element) {
		List<Integer> tmp = new ArrayList<Integer>();
		for(int i = 0; i < physicalStorage.length ; i++) {
			if (flag[i]) {
				tmp.add(physicalStorage[i]);
			}
		}
		physicalStorage = new int[2 *this.actualCapacity];
		flag = new boolean[2*this.actualCapacity];
		redistribute(0, 2 * this.actualCapacity, tmp, element);
		setParameters(2 * this.actualCapacity);
	}
	
	private void insertAtLevel(int level, int chunkNumber, int element) {
		int chunkLength = powerOfTwo(level) * this.chunkSize;
		int chunkNumberAtLevel = (chunkNumber* this.chunkSize)/chunkLength ;
		
		int startIndex = chunkNumberAtLevel * chunkLength ;
		int endIndex = startIndex + chunkLength;

		List<Integer> tmp = new ArrayList<Integer>();
		for(int i = startIndex; i < endIndex; i++) {
			if(flag[i]) {
				tmp.add(physicalStorage[i]);
				flag[i] = false;
			}
		}
		redistribute(startIndex, endIndex, tmp, element);
	}
	
	private void redistribute(int startIndex, int endIndex, List<Integer> tmp, int element) {

		double distributionRatio = (double)(endIndex - startIndex)/(double)(tmp.size()+1);
		boolean inserted = false;
		
		for( int i = 0; i < tmp.size() + 1 ; i ++) {
			int index = startIndex + (int)(i*distributionRatio);
			assert index >= startIndex && index < endIndex;
			
			if(!inserted) {
				if(i == tmp.size() || element <= tmp.get(i)) {
					physicalStorage[index] = element;
					inserted = true;
				}
				else {
					physicalStorage[index] = tmp.get(i);
				}
			}
			else {
				physicalStorage[index] = tmp.get(i-1);
			}
			flag[index] = true;
		}
		numberOfElemenetsInserted++;
	}
		
	public double upperThresholdAt(int level) {
		return 1.0 - ((HIGHEST_UPPER_DENSITY-LOWEST_UPPER_DENSITY)*level)/logBaseTwo(actualCapacity);
	}
	
	private PMAStatistics insertAtLowestChunk(int element, int chunkNumber) {
		List<Integer> tmp = new ArrayList<Integer>();
		for (int i = 0; i< chunkSize; i++) {
			if(elementPresent(chunkNumber, i)) {
				tmp.add(chunkElementAt(chunkNumber, i));
			}
		}
		
		int diff = chunkSize - tmp.size() -1 ;
		
		if(diff >= 0) {
			boolean isInserted = false; 
			for(int i = 0; i < tmp.size() + 1 ; i++) {
				if(!isInserted) {
					if(i == tmp.size() || element <= tmp.get(i)) {
						setChunkElementAt(chunkNumber, i, element);
						isInserted = true;
					}
					else {
						setChunkElementAt(chunkNumber, i, tmp.get(i));
					}
				}
				else {
					setChunkElementAt(chunkNumber, i, tmp.get(i-1));
				}
				setElementPresent(chunkNumber, i, true);
			}
			
			for( int i = 0; i < diff ; i++) {
				setElementPresent(chunkNumber, i + tmp.size() + 1, false);
			}
			
			this.numberOfElemenetsInserted++;
			return new PMAStatistics(true, tmp.size() + 1 );
		}
		return new PMAStatistics(false, tmp.size());
	}
	
	private int chunkForInsertion(int element) {
		int leftChunk = 0;
		int rightChunk = numberOfChunks - 1;
		
		while(leftChunk != rightChunk) {
			int middleChunk = ((rightChunk + leftChunk) / 2 );
			
			int index = scanChunk(middleChunk, element);
			if(index == (middleChunk + 1) * chunkSize) {
				// reached the end of the chunk 
				leftChunk = middleChunk + 1;
			}
			else {
				rightChunk = middleChunk;
			}
		}
		
		if(leftChunk > 0) {
			int adjusentLeftScan = scanChunk(leftChunk - 1, element);
			if(adjusentLeftScan == (leftChunk) * chunkSize) {
				if(elementPresent(leftChunk, 0) && chunkElementAt(leftChunk, 0) >= element) {
					return leftChunk -1;
				}
			}
		}
		return leftChunk;
	}
	
	private int scanChunk(int chunkNumber, int element) {
		int i = chunkNumber * chunkSize;
		for(;i < chunkNumber * chunkSize + chunkSize;i++) {
			if(flag[i] && physicalStorage[i] >= element) {
				return i;
			}
		}
		return i;
	}
	
	private void setChunkElementAt(int chunkNumber, int chunkIndex, int element) {
		physicalStorage[chunkNumber*chunkSize + chunkIndex] = element;
	}
	private int chunkElementAt(int chunkNumber, int chunkIndex) {
		return physicalStorage[chunkNumber*chunkSize + chunkIndex];
	}
	
	private void setElementPresent(int chunkNumber, int chunkIndex, boolean isPresent) {
		flag[chunkNumber * chunkSize + chunkIndex] = isPresent; 
	}
	
	private boolean elementPresent(int chunkNumber, int chunkIndex) {
		return flag[chunkNumber * chunkSize + chunkIndex]; 
	}
	
	public void print() {
		System.out.println("Number of elements inserted: " + numberOfElemenetsInserted);
		for(int i = 0 ; i < numberOfChunks ;i++) {
			System.out.print("Chunk: " + i + ":  ");
			
			for (int j = 0 ; j < chunkSize; j++) {
				if(flag[i * chunkSize + j]) {
					System.out.print(physicalStorage[i*chunkSize + j] + "\t");
				}
				else {
					System.out.print(" B ");
				}
			}
			System.out.println("");
		}
		System.out.println("");
	}
	
	private int powerOfTwo(int i) {
		return 1 << i;
	}
	
	private int logBaseTwo(int i) {
		int log = 0;
		while(i > 1) {
			i/=2;
			log++;
		}
		return log;
	}

	@Override
	public String toString() {
		return "PMA [actualCapacity=" + actualCapacity + ", chunkSize="
				+ chunkSize + ", numberOfChunks=" + numberOfChunks
				+ ", numberOfLevels=" + numberOfLevels
				+ ", numberOfElemenetsInserted=" + numberOfElemenetsInserted
				+ "]";
	}
	
	public List<Integer> getList() {
		if(numberOfElemenetsInserted >= BASE_CASE - 1) {
			return getPMAList();
		}
		// its a bit janky, create a new list and return !
		Collections.sort(baseCaseStorage);
		return baseCaseStorage;
	}
	
	private List<Integer> getPMAList() {
		List<Integer> tmp = new ArrayList<Integer>();
		for(int i = 0; i < physicalStorage.length ; i++) {
			if(flag[i]) {
				tmp.add(physicalStorage[i]);
			}
		}
		return tmp; 
	}
}

class PMAStatistics  {
	public boolean inLimit; 
	public int numberOfElements;
	public PMAStatistics(boolean inLimit, int numberOfElements) {
		this.inLimit = inLimit; 
		this.numberOfElements = numberOfElements;
	}
}