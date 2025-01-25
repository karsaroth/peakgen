package peakgen.generate.util;

/** Utility class providing various tuples */
public final class Tuples {
  private Tuples() {
    throw new IllegalStateException("Utility class");
  }

  public record T2<A, B>(A first, B second) {
    public static <A, B> T2<A, B> of(A first, B second) {
      return new T2<>(first, second);
    }
  }

  public record T3<A, B, C>(A first, B second, C third) {
    public static <A, B, C> T3<A, B, C> of(A first, B second, C third) {
      return new T3<>(first, second, third);
    }
  }

  public record T4<A, B, C, D>(A first, B second, C third, D fourth) {
    public static <A, B, C, D> T4<A, B, C, D> of(A first, B second, C third, D fourth) {
      return new T4<>(first, second, third, fourth);
    }
  }

  public record T5<A, B, C, D, E>(A first, B second, C third, D fourth, E fifth) {
    public static <A, B, C, D, E> T5<A, B, C, D, E> of(
        A first, B second, C third, D fourth, E fifth) {
      return new T5<>(first, second, third, fourth, fifth);
    }
  }

  public record T6<A, B, C, D, E, F>(A first, B second, C third, D fourth, E fifth, F sixth) {
    public static <A, B, C, D, E, F> T6<A, B, C, D, E, F> of(
        A first, B second, C third, D fourth, E fifth, F sixth) {
      return new T6<>(first, second, third, fourth, fifth, sixth);
    }
  }

  public record T7<A, B, C, D, E, F, G>(
      A first, B second, C third, D fourth, E fifth, F sixth, G seventh) {
    public static <A, B, C, D, E, F, G> T7<A, B, C, D, E, F, G> of(
        A first, B second, C third, D fourth, E fifth, F sixth, G seventh) {
      return new T7<>(first, second, third, fourth, fifth, sixth, seventh);
    }
  }

  public record T8<A, B, C, D, E, F, G, H>(
      A first, B second, C third, D fourth, E fifth, F sixth, G seventh, H eighth) {
    public static <A, B, C, D, E, F, G, H> T8<A, B, C, D, E, F, G, H> of(
        A first, B second, C third, D fourth, E fifth, F sixth, G seventh, H eighth) {
      return new T8<>(first, second, third, fourth, fifth, sixth, seventh, eighth);
    }
  }

  public record T9<A, B, C, D, E, F, G, H, I>(
      A first, B second, C third, D fourth, E fifth, F sixth, G seventh, H eighth, I ninth) {
    public static <A, B, C, D, E, F, G, H, I> T9<A, B, C, D, E, F, G, H, I> of(
        A first, B second, C third, D fourth, E fifth, F sixth, G seventh, H eighth, I ninth) {
      return new T9<>(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth);
    }
  }

  public record T10<A, B, C, D, E, F, G, H, I, J>(
      A first,
      B second,
      C third,
      D fourth,
      E fifth,
      F sixth,
      G seventh,
      H eighth,
      I ninth,
      J tenth) {
    public static <A, B, C, D, E, F, G, H, I, J> T10<A, B, C, D, E, F, G, H, I, J> of(
        A first,
        B second,
        C third,
        D fourth,
        E fifth,
        F sixth,
        G seventh,
        H eighth,
        I ninth,
        J tenth) {
      return new T10<>(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth);
    }
  }
}
