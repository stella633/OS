package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}
	
	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
			   
	return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
			   
	return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
	
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
			   
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	/* Fixed Lines
	 * if (priority == priorityMaximum)
	 * return false;					*/
	if (priority == priorityMaximum) {
		Machine.interrupt().restore(intStatus);
		return false;
	}
	/* Fixed Lines						*/
	
	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
	}

	public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
			   
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	/* Fixed Lines
	 * if (priority == priorityMinimum)
	 * return false;					*/
	if (priority == priorityMinimum) {
		Machine.interrupt().restore(intStatus);
		return false;
	}
	/* Fixed Lines						*/
	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;	

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
		thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
		this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
		Lib.assertTrue(Machine.interrupt().disabled());
		if (lockHolder != null) {
			lockHolder.donationQueue.remove(this);
			lockHolder.update();
		}
		ThreadState state = pickNextThread();
		if (state == null)
			return null;
		state.acquire(this);
		return state.thread;
	}
	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		// implement me
		// added ***
		Lib.assertTrue(Machine.interrupt().disabled());
		if(waitQueue.isEmpty())
			return null;
		KThread result = null;
		int maxPriority = -1;
		for (KThread thread : waitQueue)
			if (getEffectivePriority(thread) > maxPriority) {
				result = thread;
				maxPriority = getEffectivePriority(thread);
			}
		return getThreadState(result);
		// added ***
		
		// removed ***
		// return null;
		// removed ***
	}
	
	public void print() {
		Lib.assertTrue(Machine.interrupt().disabled());
		// implement me (if you want)
		// added ***
		for (Iterator i=waitQueue.iterator(); i.hasNext(); )
			System.out.print((KThread) i.next() + " ");
		System.out.println();
		// added ***
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	//added ***
	private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
	ThreadState lockHolder = null;
	//added ***
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
		this.thread = thread;
		
		setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
//test
	public int getEffectivePriority() {
		return getEffectivePriority(new HashSet<ThreadState>());
	}
	private int getEffectivePriority(HashSet<ThreadState> set) {
		if (set.contains(this)) {
			return priority;
		}

		effectivePriority = priority;

		for (PriorityQueue queue : donationQueue)
			if (queue.transferPriority)
				for (KThread thread : queue.waitQueue) {
					set.add(this);
					int p = getThreadState(thread)
							.getEffectivePriority(set);
					set.remove(this);
					if (p > effectivePriority)
						effectivePriority = p;
				}
		
		PriorityQueue queue = (PriorityQueue) thread.waitJoin;
		if (queue.transferPriority)
			for (KThread thread : queue.waitQueue) {
				set.add(this);
				int p = getThreadState(thread)
						.getEffectivePriority(set);
				set.remove(this);
				if (p > effectivePriority)
					effectivePriority = p;
			}
		 
		return effectivePriority;
}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
		if (this.priority == priority)
		return;
		
		this.priority = priority;
		
		// implement me
		// added ***
		update();
		// added ***
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
		// implement me
		// added ***
		Lib.assertTrue(Machine.interrupt().disabled());
		waitQueue.waitQueue.add(thread);
		// added ***
		//test
		if (waitQueue.lockHolder != null)
			waitQueue.lockHolder.update();
		//test
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		Lib.assertTrue(Machine.interrupt().disabled());
		//waitQueue.print();
		waitQueue.waitQueue.remove(thread);
		waitQueue.lockHolder = this;

		donationQueue.add(waitQueue);
		update();

	}	

	public void update() {
		effectivePriority = expiredEffectivePriority;
		getEffectivePriority();
	}


	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	//test
	protected int effectivePriority = expiredEffectivePriority;
	protected static final int expiredEffectivePriority = -1;
	protected LinkedList<PriorityQueue> donationQueue = new LinkedList<PriorityQueue>();
	// donation ť�� Lock Ŭ������ �Բ� ���Ǿ��µ� ���δ�.
	// effectivepriority�� integer�� null�� ���ϴ°ŵ� �ִµ� �� ����, ����ȭ
	//test
	}
}
