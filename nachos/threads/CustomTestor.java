/** 
 * Test the functionality of the PriorirtyScheduler class. Note that this
 * module must be imported into files wishing to be tested.
 *
 * Calling symantics: - Run a single test contained herein:
 * PrioritySchedulerTest.testx() for test number x.  - Run all tests contained
 * hereing: PrioritySchedulerTest.runall().
 *
 * Note that methods declared here must be static to function properly. 'make'
 * changes to this file from the respective projx directory. 
 *
 * To see DEBUGGING output on the console, run nachos with the -d x switch
 * (along with any other debugging flags, of course. 
 */
package nachos.threads;

import nachos.threads.*;
import nachos.machine.*;

public class CustomTestor {
	// ���� �ڿ� ����Ʈ
	private static Lock lock1 = new Lock();
	private static Lock conditionLock = new Lock();
	private static Lock sharedIntLock = new Lock();
	private static Semaphore mutex = new Semaphore(0);
	private static Condition cond = new Condition(conditionLock);
	private static int sharedData = 0;
	private static int sharedInteger = 0;

	// Lock�� ȹ���Ͽ� �� ������ �����Ͽ� Lock Ŭ�������� ������ ����� �����ش�.
	static class Program_1 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " ���� ����");
			Lib.debug(dbgTest, ID + " Lock 1 ȹ��!");
			lock1.acquire();

			Lib.debug(dbgTest, ID + " mutex.V() �� ȣ���Ͽ� �׽�Ʈ2(���� ������)�� Ȱ��ȭ");
			mutex.V();
			Lib.debug(dbgTest, ID + "��� ����");
			int x = 0;
			for (int i = 1; i < 123456789; i++) {
				if (i % 123456789 == 0)
					KThread.yield();
				x = x + i - (x / i);
			}
			Lib.debug(dbgTest, ID + " ��� ��, Lock 1 ��ȯ");
			sharedData = 1;
			lock1.release();
		}
	}

	static class Program_2 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " ���� ����");
			Lib.debug(dbgTest, ID + " ���α׷� 1�� Lock 1�� ���� �� �ֵ���(Ȯ���� ��������) ��� �纸");
			KThread.yield();
			Lib.debug(dbgTest, ID + " ���α׷� 1�� ������� Lock 1 ȹ�� �õ�!");
			lock1.acquire();
			Lib.assertTrue(sharedData == 1, "Expected sharedData = 1 got " + sharedData);
			Lib.debug(dbgTest, ID + " Lock 1 ȹ��! Lock 1 ��ȯ �� ���α׷� ����");
			sharedData = 2;
			lock1.release();
		}
	}

	static class Program_3 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " ���� ����");
			Lib.debug(dbgTest, ID + " ���α׷� 1�� ������� Lock 1 ȹ�� �õ�!");
			lock1.acquire();
			Lib.debug(dbgTest, ID + " Lock 1 ȹ��! Lock 1 ��ȯ �� ���α׷� ����");
			lock1.release();
		}
	}

	// �������� ���鼭 sharedInteger(�����޸�)�� ����ϰ� �ϳ��� �ٿ������� yield�� ȣ���Ѵ�.
	static class LockForkJoinLooper implements Runnable {
		private int shared_loop;

		LockForkJoinLooper(int shared){
			shared_loop = shared;
		}

		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " ���� ����");
			sharedIntLock.acquire();
			sharedInteger = shared_loop;
			Lib.debug(dbgTest, ID + " [Lock] �����޸𸮿� loopcount ����, [SharedMem] " + sharedInteger);
			sharedIntLock.release();
			Lib.debug(dbgTest, ID + " [fork] LookLooper");
			KThread auxThread = new KThread(new LockLooper(shared_loop*2))
					.setName(ID + "->LookLooper");
			auxThread.fork();

			while (sharedInteger > 0) {
				sharedIntLock.acquire();
				sharedInteger--;
				Lib.debug(dbgTest, ID + " [Lock] �����޸� -1, [SharedMem] " + sharedInteger);
				sharedIntLock.release();
				Lib.debug(dbgTest, ID + " [yield]");
				KThread.yield();
			}
			Lib.debug(dbgTest, ID + " [join] "+auxThread.getName());
			auxThread.join();
			Lib.debug(dbgTest, ID + "[Exit] ���μ��� ����");
		}
	}

	// �������� ���鼭 inner_loop�� ����ϰ� �ϳ��� �ٿ������� yield�� ȣ���Ѵ�.
	static class LockLooper implements Runnable {
		private int inner_loop;

		LockLooper(int inner) {
			inner_loop = inner;
		}

		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " [loop_remain] " + inner_loop);
			while (inner_loop > 0) {
				sharedIntLock.acquire();
				inner_loop--;
				Lib.debug(dbgTest, ID + " [Lock] inner_loop -1, [loop_remain] " + inner_loop);
				sharedIntLock.release();
				Lib.debug(dbgTest, ID + " [yield]");
				KThread.yield();
			}
			Lib.debug(dbgTest, ID + "[Exit] ���μ��� ����");
		}
	}

	// �θ�� �ڽ� ������ ���� Condition Variable ����� �����ش�.
	static class ConditionParentApplication implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			conditionLock.acquire();
			Lib.debug(dbgTest, ID + " [�θ�-ConditionLock] Condition Variable ȹ��");
			Lib.debug(dbgTest, ID + " [�θ�-fork] Child");
			KThread wakeThread = new KThread(new ConditionChildApplication()).setName("Cchild");
			wakeThread.fork();
			Lib.debug(dbgTest, ID + " [�θ�-ConditionSleep] Condition Variable�� �����带 ���");
			cond.sleep();
			Lib.debug(dbgTest, ID + " [�θ�-awaken] �ڽ� ���μ����� ����Ǿ� ���");
			Lib.debug(dbgTest, ID + " [�θ�-release] Condition Lock ��ȯ");
			conditionLock.release();
			sharedIntLock.acquire();
			Lib.debug(dbgTest, "");
			sharedInteger = 96;
			sharedIntLock.release();
			wakeThread.join();
		}
	}

	static class ConditionChildApplication implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			conditionLock.acquire();
			for(int i=0;i<123456;i++) {
				if(i==120314)
					Lib.debug(dbgTest, ID + " [�ڽ�-�ݺ��� �۾�] ������ ���� ��");
				KThread.yield();
			}
			sharedIntLock.acquire();
			Lib.debug(dbgTest, ID + " [�ڽ�-Lock] ���� �޸� Lock ȹ�� �� ����� �� ��ȯ");
			sharedInteger = 69;
			sharedIntLock.release();
			Lib.debug(dbgTest, ID + " [�ڽ�-wakeAll] Condition Lock�� ��� �����带 ����");
			cond.wakeAll();
			Lib.debug(dbgTest, ID + " [�ڽ�-release] Condition Lock ��ȯ");
			conditionLock.release();
		}
	}

	// ù��°�� �켱������ ���� ���� Round Robin �����층 ������� �����ϵ��� �ϴ� �׽�Ʈ ȯ���Դϴ�.
	// �ι�°�� �켱������ ���� �ٸ� �� Priority �����층 ������� �����մϴ�.
	// ���ο��� lock Ŭ������ �̿��� ���� �޸� �ڿ� ������ �������� fork -> join�� �����߽��ϴ�.
	// ����°�� Priority Donation �׽�Ʈ�Դϴ�.
	// �׹�°�� Condition Variable �׽�Ʈ�Դϴ�.
	public static boolean Task2Proof() {
		String ID = "[Lock, Fork, Join �׽�Ʈ1]";
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� ���ʴ�� ����");
		KThread thread1 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		KThread thread2 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		KThread thread3 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");

		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� �켱������ default(1)");
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� ������� Fork()");
		thread1.fork();
		thread2.fork();
		thread3.fork();

		thread1.join();
		Lib.debug(dbgTest, ID + " ������ 1 "+thread2.getName()+" ���� ��");
		thread2.join();
		Lib.debug(dbgTest, ID + " ������ 2 "+thread2.getName()+" ���� ��");
		thread3.join();
		Lib.debug(dbgTest, ID + " ������ 3 "+thread2.getName()+" ���� ��");
		
		ID = "[Lock, Fork, Join �׽�Ʈ2]";
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� ���ʴ�� ����");
		thread1 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 2)");
		thread2 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		thread3 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 0)");
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� �켱������ ���� 2, 1, 0");
		boolean state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 2);
		ThreadedKernel.scheduler.setPriority(thread2, 1);
		ThreadedKernel.scheduler.setPriority(thread3, 0);
		Machine.interrupt().restore(state);
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� ������� Fork()");
		thread1.fork();
		thread2.fork();
		thread3.fork();
		
		thread3.join();
		Lib.debug(dbgTest, ID + " ������ 3 "+thread3.getName()+" ������ ��");
		thread1.join();
		Lib.debug(dbgTest, ID + " ������ 1 "+thread1.getName()+" ������ ��");
		thread2.join();
		Lib.debug(dbgTest, ID + " ������ 2 "+thread2.getName()+" ������ ��");
		
		ID = "[Lock, Fork, Join Donation �׽�Ʈ]";
		
		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� ���ʴ�� ����");
		KThread prog1_thread = new KThread(new Program_1()).setName("LFJDonation_1(Priority 0)");
		KThread prog2_thread = new KThread(new Program_2()).setName("LFJDonation_2(Priority 5)");
		KThread prog3_thread = new KThread(new Program_3()).setName("LFJDonation_3(Priority 5)");

		Lib.debug(dbgTest, ID + " ������ 1, 2, 3�� �켱������ ���� 0, 5, 5");
		state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(prog1_thread, 0);
		ThreadedKernel.scheduler.setPriority(prog2_thread, 5);
		ThreadedKernel.scheduler.setPriority(prog3_thread, 5);
		Machine.interrupt().restore(state);

		Lib.debug(dbgTest, ID + " ������ 1 Fork()");
		prog1_thread.fork();

		Lib.debug(dbgTest, ID + " �������� mutex.P() ȣ���Ͽ� ���� ������ ���� ����(sleep)");
		mutex.P();

		Lib.debug(dbgTest, ID + " ������ 2, 3 Fork()");
		prog2_thread.fork();
		prog3_thread.fork();

		prog1_thread.join();
		Lib.debug(dbgTest, ID + " ������ 1 "+prog1_thread.getName()+" ���� ��");
		prog2_thread.join();
		Lib.debug(dbgTest, ID + " ������ 2 "+prog2_thread.getName()+" ���� ��");
		prog3_thread.join();
		Lib.debug(dbgTest, ID + " ������ 3 "+prog3_thread.getName()+" ���� ��");
		
		ID = "[Condition Variable �׽�Ʈ]";
		
		Lib.debug(dbgTest, ID + " �θ� ������ ����");
		KThread parent_thread = new KThread(new ConditionParentApplication()).setName("CParent");
		
		Lib.debug(dbgTest, ID + " �θ� ������ Fork()");
		parent_thread.fork();
		
		parent_thread.join();
		Lib.debug(dbgTest, ID + " ������ "+parent_thread.getName()+" ���� ��");
		
		Lib.debug(dbgTest, ID + " shared Integer ���� " + sharedInteger);
		
		return true;
	}

	// ����� ����
	private static final char dbgTest = '-';
}
