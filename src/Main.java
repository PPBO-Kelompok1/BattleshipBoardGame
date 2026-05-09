import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int row = sc.nextInt();
        int col = sc.nextInt();
        int[][] boarderid = new int[row][col];

        System.out.println();
        System.out.print("   ");
        for (int j = 0; j < col; j++) {
            System.out.printf("%-3d", j);
        }
        System.out.println();

        for (int i = 0; i < row; i++) {
            System.out.printf("%-3d", i); // row label

            for (int j = 0; j < col; j++) {
                System.out.printf("%-3s", "o");
            }
            System.out.println();
        }

        sc.close();

        // tes push: Angga
    }
}