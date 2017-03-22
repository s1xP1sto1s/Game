package net.cpszju.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Game {
	private static final int NUM_THREAD = 10;
	public static void main(String[] args){
		ExecutorService es = Executors.newFixedThreadPool(NUM_THREAD); 	
		int[][] map;
		//输入地图大小，错误重新输入
		while(true){
			Future<int[]> future = es.submit(new GetXY());
			int[] loc;
			try {
				loc = future.get();
				if(loc[2]==1){
					map = new int[loc[0]][loc[1]];
					break;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				future.cancel(true);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		//输入障碍物坐标，错误重新输入
		while(true){
			Future<List<Integer[]>> future = es.submit(new GetBarrierXY());
			List<Integer[]> loc;
			try {
				loc = future.get();
				if(loc!=null){
					for(Integer[] i:loc){
						map[i[0]][i[1]] = -1;
					}
					break;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				future.cancel(true);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		//游戏开始
		GameRun gameRun = new GameRun(map);
		es.execute(gameRun);
		System.out.println("游戏开始！");
		//一个用户开始玩
		Play play = new Play(gameRun);
		es.execute(play);
		while(!play.getFlag()){
			es.shutdown();
		}
	}
}

class GetXY implements Callable<int[]>{
	public int[] call() throws Exception {
		Scanner sc = new Scanner(System.in);
		System.out.println("请输入游戏区域，数字之间以空格分隔");
		String[] loc = sc.nextLine().split(" ");
		int[] res = new int[3];
		try{
			res[0] = Integer.parseInt(loc[0]);
			res[1] = Integer.parseInt(loc[1]);
			res[2] = 1;
		}catch(NumberFormatException nfe){
			res[2] = 0;
			System.out.println("您的输入不合法，请重新输入");
		}
		return res;
	}
}

class GetBarrierXY implements Callable<List<Integer[]>>{
	public List<Integer[]> call() throws Exception {
		Scanner sc = new Scanner(System.in);
		System.out.println("请输入障碍物坐标，坐标之间逗号分隔，每组之间分号分隔");
		String[] loc = sc.nextLine().split(";");
		List<Integer[]> list = new ArrayList<Integer[]>();
		Integer[] res;
		for(int i=0;i<loc.length;i++){
			res = new Integer[2];
			try{
				res[0] = Integer.parseInt(loc[i].split(",")[0]);
				res[1] = Integer.parseInt(loc[i].split(",")[1]);
				list.add(res);
			}catch(NumberFormatException nfe){
				System.out.println("您的输入不合法，请重新输入");
				return null;
			}
		}
		return list;
	}
}

class Play implements Runnable{
	private volatile boolean flag = false;
	private GameRun gameRun;
	
	public Play(GameRun gameRun){
		this.gameRun = gameRun;
	}
	
	public boolean getFlag(){
		return flag;
	}
	
	public void run() {
		Scanner sc = new Scanner(System.in);
		while(!gameRun.getFlag()){
			System.out.println("请输入：W S A D J K 玩游戏");
			String line = sc.nextLine();
			if("W".equals(line)){
				int[] pMan = gameRun.GetMan();
				int[] status = gameRun.getStatus();
				if(status[0]==3){
					System.out.println("前方死路，无法移动");
				}
				else if(status[0]==2){
					System.out.println("前方有障碍物，无法移动");
				}
				else{
					pMan[1]--;
					gameRun.setPMan(pMan);
				}
			}
			else if("S".equals(line)){
				int[] pMan = gameRun.GetMan();
				int[] status = gameRun.getStatus();
				if(status[1]==3){
					System.out.println("下方死路，无法移动");
				}
				else if(status[1]==2){
					System.out.println("下方有障碍物，无法移动");
				}
				else{
					pMan[1]++;
					gameRun.setPMan(pMan);
				}
			}
			else if("A".equals(line)){
				int[] pMan = gameRun.GetMan();
				int[] status = gameRun.getStatus();
				if(status[2]==3){
					System.out.println("左方死路，无法移动");
				}
				else if(status[2]==2){
					System.out.println("左方有障碍物，无法移动");
				}
				else{
					pMan[0]--;
					gameRun.setPMan(pMan);
				}
			}
			else if("D".equals(line)){
				int[] pMan = gameRun.GetMan();
				int[] status = gameRun.getStatus();
				if(status[3]==3){
					System.out.println("右方死路，无法移动");
				}
				else if(status[3]==2){
					System.out.println("右方有障碍物，无法移动");
				}
				else{
					pMan[0]++;
					gameRun.setPMan(pMan);
				}
			}
			else if("J".equals(line)){
				int[] status = gameRun.getStatus();
				String[] str = new String[]{"上:","下:","左:","右:"};
				for(int i=0;i<status.length;i++){
					if(status[i]==0)
						System.out.println(str[i]+"可以走呀");
					else if(status[i]==1)
						System.out.println(str[i]+"有怪兽呀");
					else if(status[i]==2)
						System.out.println(str[i]+"有障碍物呀");
					else 
						System.out.println(str[i]+"到边界了呀");
				}
			}
			else if("K".equals(line)){
				int[] pMan = gameRun.GetMan();
				System.out.println("您当前的位置是：X："+pMan[0]+" Y："+pMan[1]);
			}
			else{
				System.out.println("输入错误！");
			}
		}
		flag = true;
	}
}

class GameRun implements Runnable{
	private volatile boolean flag = false;
	//地图
	private int[][] map;
	//人的坐标,先设一个默认起始位置
	private int[] pMan = new int[]{0,0};
	//怪物的坐标，假设只有一个怪物先
	private int[] pMon = new int[]{1,1};
	
	public boolean getFlag(){
		return flag;
	}
	
	public GameRun(int[][] map){
		this.map = map;
	}
	
	public void setPMan(int[] p){
		this.pMan[0] = p[0];
		this.pMan[1] = p[1];
	}
	
	public int[] GetMan(){
		return pMan;
	}
	
	public int[] getStatus(){
		//0表示什么都没有,1表示有怪物,2表示有障碍物,3表示走到了尽头
		//数组长度为4，每一位对应上下左右的状态
		int[] status = new int[4];
		int x = pMan[0];
		int y = pMan[1];
		
		int upX = x;
		int upY = y-1;
		int downX = x;
		int downY = y+1;
		int leftX = x-1;
		int leftY = y;
		int rightX = x+1;
		int rightY = y;
		//判断上方状态
		if(upY==-1)
			status[0] = 3;
		else if(map[upX][upY]==0)
			status[0] = 0;
		else if(map[upX][upY]==-1)
			status[0] = 2;
		else if(upX==pMon[0] && upY==pMon[1])
			status[0] = 1;
		//判断下方状态
		if(downY==map.length)
			status[1] = 3;
		else if(map[downX][downY]==0)
			status[1] = 0;
		else if(map[downX][downY]==-1)
			status[1] = 2;
		else if(downX==pMon[0] && downY==pMon[1])
			status[1] = 1;
		//判断左方状态
		if(leftX==-1)
			status[2] = 3;
		else if(map[leftX][leftY]==0)
			status[2] = 0;
		else if(map[leftX][leftY]==-1)
			status[2] = 2;
		else if(leftX==pMon[0] && leftY==pMon[1])
			status[2] = 1;
		//判断右方状态
		if(rightX==map[0].length)
			status[3] = 3;
		else if(map[rightX][rightY]==0)
			status[3] = 0;
		else if(map[rightX][rightY]==-1)
			status[3] = 2;
		else if(rightX==pMon[0] && rightY==pMon[1])
			status[3] = 1;
			
		return status;
	}
	
	public void run() {
		while(true){
			//怪物每一秒钟更新一次位置
			int number = new Random().nextInt(4);
			switch(number){
				//如果是0，怪物向上走
				case 0:{
					int y = pMon[1];
					if(y>0 && map[pMon[0]][y-1]!=-1)
						pMon[1]--;
					break;
				}
				//如果是1，怪物向下走
				case 1:{
					int y = pMon[1];
					if(y<map.length-1 && map[pMon[0]][y+1]!=-1)
						pMon[1]++;
					break;
				}
				//如果是2，怪物向左走
				case 2:{
					int x = pMon[0];
					if(x>0 && map[x-1][pMon[1]]!=-1)
						pMon[0]--;
				}
				//如果是3，怪物向右走
				case 3:{
					int x = pMon[0];
					if(x<map[0].length-1 && map[x+1][pMon[1]]!=-1)
						pMon[0]++;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(pMon[0]==pMan[0] && pMon[1]==pMan[1]){
				System.out.println("你被怪物吃掉啦！！！游戏结束");
				flag = true;
				return;
			}
		}
	}
}