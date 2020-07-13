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
	// 공유 자원 리스트
	private static Lock lock1 = new Lock();
	private static Lock conditionLock = new Lock();
	private static Lock sharedIntLock = new Lock();
	private static Semaphore mutex = new Semaphore(0);
	private static Condition cond = new Condition(conditionLock);
	private static int sharedData = 0;
	private static int sharedInteger = 0;

	// Lock을 획득하여 긴 연산을 수행하여 Lock 클래스와의 유연한 사용을 보여준다.
	static class Program_1 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " 실행 시작");
			Lib.debug(dbgTest, ID + " Lock 1 획득!");
			lock1.acquire();

			Lib.debug(dbgTest, ID + " mutex.V() 를 호출하여 테스트2(메인 스레드)를 활성화");
			mutex.V();
			Lib.debug(dbgTest, ID + "계산 진행");
			int x = 0;
			for (int i = 1; i < 123456789; i++) {
				if (i % 123456789 == 0)
					KThread.yield();
				x = x + i - (x / i);
			}
			Lib.debug(dbgTest, ID + " 계산 끝, Lock 1 반환");
			sharedData = 1;
			lock1.release();
		}
	}

	static class Program_2 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " 실행 시작");
			Lib.debug(dbgTest, ID + " 프로그램 1이 Lock 1을 가질 수 있도록(확실히 가지도록) 잠시 양보");
			KThread.yield();
			Lib.debug(dbgTest, ID + " 프로그램 1이 사용중인 Lock 1 획득 시도!");
			lock1.acquire();
			Lib.assertTrue(sharedData == 1, "Expected sharedData = 1 got " + sharedData);
			Lib.debug(dbgTest, ID + " Lock 1 획득! Lock 1 반환 후 프로그램 종료");
			sharedData = 2;
			lock1.release();
		}
	}

	static class Program_3 implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " 실행 시작");
			Lib.debug(dbgTest, ID + " 프로그램 1이 사용중인 Lock 1 획득 시도!");
			lock1.acquire();
			Lib.debug(dbgTest, ID + " Lock 1 획득! Lock 1 반환 후 프로그램 종료");
			lock1.release();
		}
	}

	// 루프문을 돌면서 sharedInteger(공유메모리)를 출력하고 하나씩 줄여나가며 yield를 호출한다.
	static class LockForkJoinLooper implements Runnable {
		private int shared_loop;

		LockForkJoinLooper(int shared){
			shared_loop = shared;
		}

		public void run() {
			String ID = KThread.currentThread().toString();
			Lib.debug(dbgTest, ID + " 실행 시작");
			sharedIntLock.acquire();
			sharedInteger = shared_loop;
			Lib.debug(dbgTest, ID + " [Lock] 공유메모리에 loopcount 저장, [SharedMem] " + sharedInteger);
			sharedIntLock.release();
			Lib.debug(dbgTest, ID + " [fork] LookLooper");
			KThread auxThread = new KThread(new LockLooper(shared_loop*2))
					.setName(ID + "->LookLooper");
			auxThread.fork();

			while (sharedInteger > 0) {
				sharedIntLock.acquire();
				sharedInteger--;
				Lib.debug(dbgTest, ID + " [Lock] 공유메모리 -1, [SharedMem] " + sharedInteger);
				sharedIntLock.release();
				Lib.debug(dbgTest, ID + " [yield]");
				KThread.yield();
			}
			Lib.debug(dbgTest, ID + " [join] "+auxThread.getName());
			auxThread.join();
			Lib.debug(dbgTest, ID + "[Exit] 프로세스 종료");
		}
	}

	// 루프문을 돌면서 inner_loop를 출력하고 하나씩 줄여나가며 yield를 호출한다.
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
			Lib.debug(dbgTest, ID + "[Exit] 프로세스 종료");
		}
	}

	// 부모와 자식 스레드 간의 Condition Variable 사용을 보여준다.
	static class ConditionParentApplication implements Runnable {
		public void run() {
			String ID = KThread.currentThread().toString();
			conditionLock.acquire();
			Lib.debug(dbgTest, ID + " [부모-ConditionLock] Condition Variable 획득");
			Lib.debug(dbgTest, ID + " [부모-fork] Child");
			KThread wakeThread = new KThread(new ConditionChildApplication()).setName("Cchild");
			wakeThread.fork();
			Lib.debug(dbgTest, ID + " [부모-ConditionSleep] Condition Variable로 스레드를 재움");
			cond.sleep();
			Lib.debug(dbgTest, ID + " [부모-awaken] 자식 프로세스가 종료되어 깨어남");
			Lib.debug(dbgTest, ID + " [부모-release] Condition Lock 반환");
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
					Lib.debug(dbgTest, ID + " [자식-반복문 작업] 스레드 진행 중");
				KThread.yield();
			}
			sharedIntLock.acquire();
			Lib.debug(dbgTest, ID + " [자식-Lock] 공유 메모리 Lock 획득 후 사용한 뒤 반환");
			sharedInteger = 69;
			sharedIntLock.release();
			Lib.debug(dbgTest, ID + " [자식-wakeAll] Condition Lock의 모든 스레드를 깨움");
			cond.wakeAll();
			Lib.debug(dbgTest, ID + " [자식-release] Condition Lock 반환");
			conditionLock.release();
		}
	}

	// 첫번째는 우선순위가 전부 같아 Round Robin 스케쥴링 방식으로 동작하도록 하는 테스트 환경입니다.
	// 두번째는 우선순위가 전부 다를 때 Priority 스케쥴링 방식으로 동작합니다.
	// 내부에는 lock 클래스를 이용한 공유 메모리 자원 관리와 스레드의 fork -> join을 구현했습니다.
	// 세번째는 Priority Donation 테스트입니다.
	// 네번째는 Condition Variable 테스트입니다.
	public static boolean Task2Proof() {
		String ID = "[Lock, Fork, Join 테스트1]";
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3을 차례대로 생성");
		KThread thread1 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		KThread thread2 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		KThread thread3 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");

		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3의 우선순위는 default(1)");
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3을 순서대로 Fork()");
		thread1.fork();
		thread2.fork();
		thread3.fork();

		thread1.join();
		Lib.debug(dbgTest, ID + " 스레드 1 "+thread2.getName()+" 실행 끝");
		thread2.join();
		Lib.debug(dbgTest, ID + " 스레드 2 "+thread2.getName()+" 실행 끝");
		thread3.join();
		Lib.debug(dbgTest, ID + " 스레드 3 "+thread2.getName()+" 실행 끝");
		
		ID = "[Lock, Fork, Join 테스트2]";
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3을 차례대로 생성");
		thread1 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 2)");
		thread2 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 1)");
		thread3 = new KThread(new LockForkJoinLooper(10)).setName("LFJLooper(Priority 0)");
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3의 우선순위는 각각 2, 1, 0");
		boolean state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 2);
		ThreadedKernel.scheduler.setPriority(thread2, 1);
		ThreadedKernel.scheduler.setPriority(thread3, 0);
		Machine.interrupt().restore(state);
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3을 순서대로 Fork()");
		thread1.fork();
		thread2.fork();
		thread3.fork();
		
		thread3.join();
		Lib.debug(dbgTest, ID + " 스레드 3 "+thread3.getName()+" 실행이 끝");
		thread1.join();
		Lib.debug(dbgTest, ID + " 스레드 1 "+thread1.getName()+" 실행이 끝");
		thread2.join();
		Lib.debug(dbgTest, ID + " 스레드 2 "+thread2.getName()+" 실행이 끝");
		
		ID = "[Lock, Fork, Join Donation 테스트]";
		
		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3을 차례대로 생성");
		KThread prog1_thread = new KThread(new Program_1()).setName("LFJDonation_1(Priority 0)");
		KThread prog2_thread = new KThread(new Program_2()).setName("LFJDonation_2(Priority 5)");
		KThread prog3_thread = new KThread(new Program_3()).setName("LFJDonation_3(Priority 5)");

		Lib.debug(dbgTest, ID + " 스레드 1, 2, 3의 우선순위는 각각 0, 5, 5");
		state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(prog1_thread, 0);
		ThreadedKernel.scheduler.setPriority(prog2_thread, 5);
		ThreadedKernel.scheduler.setPriority(prog3_thread, 5);
		Machine.interrupt().restore(state);

		Lib.debug(dbgTest, ID + " 스레드 1 Fork()");
		prog1_thread.fork();

		Lib.debug(dbgTest, ID + " 세마포어 mutex.P() 호출하여 메인 스레드 실행 중지(sleep)");
		mutex.P();

		Lib.debug(dbgTest, ID + " 스레드 2, 3 Fork()");
		prog2_thread.fork();
		prog3_thread.fork();

		prog1_thread.join();
		Lib.debug(dbgTest, ID + " 스레드 1 "+prog1_thread.getName()+" 실행 끝");
		prog2_thread.join();
		Lib.debug(dbgTest, ID + " 스레드 2 "+prog2_thread.getName()+" 실행 끝");
		prog3_thread.join();
		Lib.debug(dbgTest, ID + " 스레드 3 "+prog3_thread.getName()+" 실행 끝");
		
		ID = "[Condition Variable 테스트]";
		
		Lib.debug(dbgTest, ID + " 부모 스레드 생성");
		KThread parent_thread = new KThread(new ConditionParentApplication()).setName("CParent");
		
		Lib.debug(dbgTest, ID + " 부모 스레드 Fork()");
		parent_thread.fork();
		
		parent_thread.join();
		Lib.debug(dbgTest, ID + " 스레드 "+parent_thread.getName()+" 실행 끝");
		
		Lib.debug(dbgTest, ID + " shared Integer 값은 " + sharedInteger);
		
		return true;
	}

	// 디버깅 변수
	private static final char dbgTest = '-';
}
