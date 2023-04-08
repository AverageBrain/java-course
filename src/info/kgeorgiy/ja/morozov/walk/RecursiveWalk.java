package info.kgeorgiy.ja.morozov.walk;


public class RecursiveWalk {
    public static void main(String[] args) {
        BaseWalk baseWalk = new BaseWalk();
        baseWalk.run(args, BaseWalk.TypeWalk.RECURSIVE);
    }
}
