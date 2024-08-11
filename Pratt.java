import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class Pratt {

  public static enum TokenKind implements Predicate<TokenKind> {
    None, Num,
    /**
     * 加
     */
    Add("+"),
    /**
     * 减
     */
    Sub("-"),
    /**
     * 乘
     */
    Star("*"),
    /**
     * 除
     */
    Slash("/"),
    /**
     * 求余
     */
    Percent("%"), //
    /**
     * 按位与
     */
    Amp("&"),
    /**
     * 按位或
     */
    Bar("|"),
    /**
     * 按位非
     */
    Tilde("~"),
    /**
     * 按位异或
     */
    Caret("^"),
    /**
     * 左移
     */
    LS("<<"),
    /**
     * 右移
     */
    RS(">>"),
    /**
     * 阶乘
     */
    Bang("!"),
    /**
     * 左小括号
     */
    Lpa("("),
    /**
     * 右小括号
     */
    Rpa(")"), //
    Eof,
    ;

    public final String operator;

    TokenKind() {
      this(null);
    }

    TokenKind(String op) {
      this.operator = op;
    }

    public int getPrecedence() {
      switch (this) {
        case Bang -> {
          return 100;
        }
        case Star, Slash, Percent -> {
          return 90;
        }
        case Add, Sub -> {
          return 80;
        }
        case LS, RS -> {
          return 70;
        }
        case Amp -> {
          return 60;
        }
        case Bar -> {
          return 50;
        }
        case Tilde -> {
          return 40;
        }
        default -> {
          return 0;
        }
      }
    }

    public boolean isPrefix() {
      return this == Add || this == Sub || this == Tilde;
    }

    public boolean isInfix() {
      return this == Add || this == Sub || this == Star || this == Slash || this == Percent || this == Amp || this == Bar || this == Caret || this == LS || this == RS;
    }

    public boolean isPostfix() {
      return this == Bang;
    }

    public boolean isValue() {
      return this == Num;
    }

    public boolean isLeftAssociative() {
      return this == Add || this == Sub || this == Tilde;
    }

    public boolean isRightAssociative() {
      return this == Bang;
    }

    @Override
    public boolean test(TokenKind that) {
      return this == that;
    }

  }


  public static class Token {

    public static final Token DUM_TOKEN = new Token(TokenKind.None, "", -1, -1);
    public static final Token EOI_TOKEN = new Token(TokenKind.Eof, "", -1, -1);

    public static final byte EOF = 0x1A;

    private final TokenKind kind;
    private final String value;
    private final int column;
    private final int line;


    public Token(TokenKind kind, String value, int column, int line) {
      this.kind = Objects.requireNonNull(kind);
      this.value = value;
      this.column = column;
      this.line = line;
    }

    @Override
    public String toString() {
      return kind == TokenKind.Eof ? TokenKind.Eof.name() : value;
    }

  }


  public static interface Visitor<R, C> {

    R visit(ValueNode n, C ctx);

    R visit(PrefixOpNode n, C ctx);

    R visit(InfixOpNode n, C ctx);

    R visit(PostfixOpNode n, C ctx);
  }


  public static abstract class ExprNode {

    public abstract <R, C> R accept(Visitor<R, C> v, C ctx);

    @Override
    public abstract String toString();
  }


  public static class ValueNode extends ExprNode {
    private final String value;

    public ValueNode(String value) {
      this.value = value;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> v, C ctx) {
      return v.visit(this, ctx);
    }

    @Override
    public String toString() {
      return value;
    }
  }


  public static class PrefixOpNode extends ExprNode {
    private final Token token;
    private final ExprNode rhs;

    public PrefixOpNode(Token token, ExprNode rhs) {
      this.token = token;
      this.rhs = rhs;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> v, C ctx) {
      return v.visit(this, ctx);
    }

    @Override
    public String toString() {
      return token.value + rhs.toString();
    }
  }


  public static class InfixOpNode extends ExprNode {
    private final ExprNode lhs;
    private final Token token;
    private final ExprNode rhs;

    public InfixOpNode(ExprNode lhs, Token token, ExprNode rhs) {
      this.lhs = lhs;
      this.token = token;
      this.rhs = rhs;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> v, C ctx) {
      return v.visit(this, ctx);
    }

    @Override
    public String toString() {
      return lhs.toString() + token.value + rhs.toString();
    }

  }


  public static class PostfixOpNode extends ExprNode {
    private final ExprNode lhs;
    private final Token token;

    public PostfixOpNode(ExprNode lhs, Token token) {
      this.lhs = lhs;
      this.token = token;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> v, C ctx) {
      return v.visit(this, ctx);
    }

    @Override
    public String toString() {
      return lhs.toString() + token.value;
    }
  }


  public static class Lexer implements Iterable<Token>, Iterator<Token> {

    private final Tokenizer tokenizer;
    private final List<Token> tokens;
    private Token prevToken;
    private Token token;
    private int index;

    public Lexer(String input) {
      this.tokenizer = new Tokenizer(input);
      this.tokens = new ArrayList<>();
      this.prevToken = this.token = Token.DUM_TOKEN;
    }

    @Override
    public Iterator<Token> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return this.tokenizer.isAvailable();
    }

    @Override
    public Token next() {
      this.prevToken = this.token;
      if (!this.tokens.isEmpty()) {
        this.token = this.tokens.removeFirst();
      } else {
        this.token = tokenizer.readToken();
      }
      this.index++;
      return this.token;
    }

    public int getIndex() {
      return this.index;
    }

    public Token getPrevToken() {
      return prevToken;
    }

    public void setPrevToken(Token prevToken) {
      this.prevToken = prevToken;
    }

    public Token token() {
      return token(0);
    }

    public Token token(int lookahead) {
      if (lookahead == 0) {
        return this.token;
      } else {
        ensureLookahead(lookahead);
        return tokens.get(lookahead - 1);
      }
    }

    private void ensureLookahead(int lookahead) {
      for (int i = tokens.size(); i < lookahead; i++) {
        tokens.add(tokenizer.readToken());
      }
    }
  }


  public static class Tokenizer {

    private final String input;
    private int pos;
    private int line;
    private TokenKind tk;
    private char ch;
    private int radix;
    private StringBuilder buf = new StringBuilder();
    private boolean eof = false;

    public Tokenizer(String input) {
      this.input = input;
      this.ch = input.charAt(this.pos);
    }

    public Token readToken() {
      String value;
      Token token;
      int col;
      int line;
      loop:
      while (true) {
        col = this.pos;
        line = this.line;
        switch (next()) {
          case Token.EOF -> {
            if (eof) {
              this.tk = TokenKind.Eof;
            }
            break loop;
          }
          // 空白
          case ' ', '\t', '\f' -> {
            continue;
          }
          case '\r', '\n' -> {
            this.line++;
            continue;
          }
          // 数字
          case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
            this.tk = TokenKind.Num;
            backup();
            scanNum();
            break loop;
          }

          case '+' -> {
            this.tk = TokenKind.Add;
            break loop;
          }
          case '-' -> {
            this.tk = TokenKind.Sub;
            break loop;
          }
          case '*' -> {
            this.tk = TokenKind.Star;
            break loop;
          }
          case '/' -> {
            this.tk = TokenKind.Slash;
            break loop;
          }
          // 求余
          case '%' -> {
            this.tk = TokenKind.Percent;
            break loop;
          }
          // 阶乘
          case '!' -> {
            this.tk = TokenKind.Bang;
            break loop;
          }
          // 位运算:异或
          case '^' -> {
            this.tk = TokenKind.Caret;
            break loop;
          }
          case '&' -> {
            this.tk = TokenKind.Amp;
            break loop;
          }
          case '|' -> {
            this.tk = TokenKind.Bar;
            break loop;
          }
          case '~' -> {
            this.tk = TokenKind.Tilde;
            break loop;
          }
          // 左移/右移
          case '<' -> {
            consume('<');
            this.tk = TokenKind.LS;
            break loop;
          }
          case '>' -> {
            consume('>');
            this.tk = TokenKind.RS;
            break loop;
          }
          // 括号
          case '(' -> {
            this.tk = TokenKind.Lpa;
            break loop;
          }
          case ')' -> {
            this.tk = TokenKind.Rpa;
            break loop;
          }
          default -> throw new IllegalArgumentException("unknown char: " + ch);
        }
      }
      value = eof ? "eof" : this.input.substring(col, this.pos);
      token = new Token(this.tk, value, col, line);
      return token;
    }

    public boolean isAvailable() {
      return this.pos <= this.input.length();
    }

    private char next() {
      if (this.pos >= this.input.length()) {
        this.eof = true;
        return Token.EOF;
      } else {
        this.ch = this.input.charAt(this.pos);
        this.pos++;
        if (this.ch == '\r' || this.ch == '\n') {
          this.line++;
        }
        return this.ch;
      }
    }

    private char peek() {
      char ch = next();
      backup();
      return ch;
    }

    private void consume(char ch) {
      if (ch != next()) {
        throw new IllegalArgumentException("expect: " + ch + ", actual:" + get());
      }
    }

    private char get() {
      return this.ch;
    }

    private boolean accept(String valid) {
      if (valid.indexOf(next()) >= 0) {
        return true;
      }
      backup();
      return false;
    }

    private void backup() {
      if (!this.eof && this.pos > 0) {
        char ch = this.input.charAt(this.pos - 1);
        this.pos--;
        if (ch == '\r' || ch == '\n') {
          this.line--;
        }
      }
    }

    private void acceptRun(String valid) {
      for (char ch = next(); valid.indexOf(ch) >= 0; ch = next()) {
        buf.append(ch);
      }
      backup();
    }

    private void scanNum() {
      buf = new StringBuilder();
      accept("+-");
      // Is it hex?
      this.radix = 10;
      var digits = "0123456789_";
      if (accept("0")) {
        // Note: Leading 0 does not mean octal in floats.
        if (accept("xX")) {
          this.radix = 16;
          digits = "0123456789abcdefABCDEF_";
        } else if (accept("oO")) {
          this.radix = 8;
          digits = "01234567_";
        } else if (accept("bB")) {
          this.radix = 2;
          digits = "01_";
        }
      }
      acceptRun(digits);
      if (accept(".")) {
        acceptRun(digits);
      }
    }
  }


  /**
   * <pre>
   *   lbp (left binding power) : 左结合运算符
   *   rbp (right binding power) : 右结合运算符
   *   nud: 一元运算符
   *   led: 二元运算符
   * </pre>
   */
  public static class Parser {

    private final Lexer lexer;

    public Parser(Lexer lexer) {
      this.lexer = lexer;
    }

    public ExprNode parse() {
      return parse(0);
    }

    private ExprNode parse(int rbp) {
      ExprNode left = this.factor();
      ExprNode old;
      var curr = this.lexer.token();
      var next = this.lexer.next();
      left = nud(curr, left);
      while (rbp < next.kind.getPrecedence()) {
        old = left;
        left = this.led(left);
        if (left == null) {
          return old;
        }
        next = this.lexer.token();
      }
      return left;
    }

    /**
     * <pre>
     *  expr -> term {(+|-) term}
     *  term -> factor {(*|/) factor}
     *  factor -> number | ( expr )
     *  number -> digit {digit}
     *  digit -> 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
     * </pre>
     */
    private ExprNode factor() {
      var token = this.lexer.next();
      switch (token.kind) {
        case Eof -> {
          return null;
        }
        case Lpa -> {
          ExprNode left = parse(0);
          consume(TokenKind.Rpa);
          return left;
        }
        case Num -> {
          return new ValueNode(token.value);
        }
        default -> throw new IllegalArgumentException("Unknown token: " + token);
      }
    }

    private PrefixOpNode parsePrefixExpr() {
      return new PrefixOpNode(null, null);
    }

    private InfixOpNode parseInfixExpr() {
      return new InfixOpNode(null, null, null);
    }

    private PostfixOpNode parsePostfixExpr() {
      return new PostfixOpNode(null, null);
    }

    private void consume(TokenKind tk) {
      var curr = this.lexer.next();
      if (curr.kind != tk) {
        throw new IllegalArgumentException();
      }
    }

    /**
     * 一元运算符
     *
     * @return 表达式
     */
    private ExprNode nud(Token prev, ExprNode left) {
      // -1,3!
      var op = lexer.token();
      if (op.kind == TokenKind.Eof) {
        return left;
      }
      ExprNode node;
      if (prev.kind.isPrefix() && !op.kind.isPrefix()) {
        // 前缀: -1
        node = new PrefixOpNode(op, this.parse(op.kind.getPrecedence() - 1));
      } else if (op.kind.isPostfix()) {
        // 后缀: 4!
        node = new PostfixOpNode(left, op);
        this.lexer.next();
      } else {
        node = left;
      }
      return node;
    }

    /**
     * 二元运算符
     *
     * @return 表达式
     */
    private ExprNode led(ExprNode left) {
      var op = lexer.token();
      if (op.kind == TokenKind.Eof) {
        return left;
      }
      ExprNode node;
      if (op.kind.isRightAssociative()) {
        node = new InfixOpNode(left, op, this.parse(op.kind.getPrecedence() - 1));
      } else {
        node = new InfixOpNode(left, op, this.parse(op.kind.getPrecedence()));
      }
      return node;
    }

  }


  public static class Calculator implements Visitor<Double, Void> {

    @Override
    public Double visit(ValueNode n, Void ctx) {
      return Double.parseDouble(n.value);
    }

    @Override
    public Double visit(PrefixOpNode n, Void ctx) {
      var rhs = n.rhs.accept(this, ctx);
      double v;
      switch (n.token.kind) {
        case Sub -> v = -rhs;
        case Add -> v = rhs;
        case Tilde -> v = ~rhs.intValue();
        default -> throw new IllegalArgumentException("unknown token:" + n.token);
      }
      return v;
    }

    @Override
    public Double visit(InfixOpNode n, Void ctx) {
      var lhs = n.lhs.accept(this, ctx);
      var rhs = n.rhs.accept(this, ctx);
      double v;
      switch (n.token.kind) {
        case Add -> v = lhs + rhs;
        case Sub -> v = lhs - rhs;
        case Star -> v = lhs * rhs;
        case Slash -> v = lhs / rhs;
        case Percent -> v = lhs % rhs;
        case Amp -> v = lhs.intValue() & rhs.intValue();
        case Bar -> v = lhs.intValue() | rhs.intValue();
        case Caret -> v = lhs.intValue() ^ rhs.intValue();
        case LS -> v = lhs.intValue() << rhs.intValue();
        case RS -> v = lhs.intValue() >> rhs.intValue();
        default -> throw new IllegalArgumentException("unknown token:" + n.token);
      }
      return v;
    }

    @Override
    public Double visit(PostfixOpNode n, Void ctx) {
      var lhs = n.lhs.accept(this, ctx);
      switch (n.token.kind) {
        case Bang -> {
          int x = lhs.intValue();
          double num = 1;
          for (int i = 1; i <= x; i++) {
            num *= i;
          }
          return num;
        }
        default -> throw new IllegalArgumentException("unknown token:" + n.token);
      }
    }
  }

  public static void main(String[] args) {
    // 正则: Thompson算法 -> NFA
    // NFA：子集构造算法 -> DFA
    // DFA: Hopcroft最小化算法 -> 词法分析器
    var lexer = new Lexer("305 << 2 - 212 + 4 * 5!");
    var parser = new Parser(lexer);
    var expr = parser.parse();
    var calculator = new Calculator();
    System.out.println(expr.accept(calculator, null));
  }

}
