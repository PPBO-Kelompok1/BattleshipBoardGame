package Kapal;

public abstract class Kapal {
    protected String nama;
    protected int hp;
    protected int damage;
    protected int x;
    protected int y;
    protected boolean hancur; // untuk status kapal apakah hancur atau tidak

    public Kapal(String nama, int hp, int damage, int x, int y) {
        this.nama = nama;
        this.hp = hp;
        this.damage = damage;
        this.x = x;
        this.y = y;
        this.hancur = false;
    }

    public void kenaSerang(int dmg) {
        hp -= dmg;

        System.out.println(nama + " terkena serangan sebesar " + dmg);

        if (hp <= 0) {
            hp = 0;
            hancur = true;
            System.out.println(nama + " telah hancur!");
        }
    }

    public boolean isHancur() {
        return hancur;
    }

    public void tampilInfo() {
        System.out.println("Nama Kapal : " + nama);
        System.out.println("HP         : " + hp);
        System.out.println("Damage     : " + damage);
        System.out.println("Posisi     : (" + x + ", " + y + ")");
    }

    public void pindahPosisi(int xBaru, int yBaru) {
        this.x = xBaru;
        this.y = yBaru;
    }

    public String getNama() {
        return nama;
    }

    public int getHp() {
        return hp;
    }

    public int getDamage() {
        return damage;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public abstract void kemampuan();

}
