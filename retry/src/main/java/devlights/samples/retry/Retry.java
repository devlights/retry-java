package devlights.samples.retry;

import java.util.ArrayList;
import java.util.List;

/**
 * リトライ処理を汎用的に行えるユーティリティクラスです。<br>
 * {@link #execute(int, int, Runnable)}に対して{@link Runnable}を指定すると<br>
 * 指定されたリトライ回数とインターバルで処理をリトライ付きで実行します。<br>
 * {@link #execute(int, int, Runnable, ErrorCallback)}を利用することで<br>
 * 処理実行時にエラーが発生した場合にエラーコールバックが呼ばれるようにすることもできます。<br>
 * <br>
 * [作成する際に参考にした情報]<br>
 * <ul>
 * <li>http://d.hatena.ne.jp/Yoshiori/20120315/1331825419</li>
 * <li>https://github.com/yoshiori/retry-handler</li>
 * </ul>
 * 
 * @author devlights
 *
 */
public final class Retry {

  /**
   * プライベートコンストラクタ<br>
   */
  private Retry() {
    // nop.
  }
  
  /**
   * 指定された情報を元にリトライ処理付きでRunnableを実行します。<br>
   * 処理が試行される回数は、（一度目の実行 + リトライ回数）です。<br>
   * エラーが発生すると呼び元に{@link RetryException}がスローされます。<br>
   * 
   * @param retryCount リトライ回数
   * @param interval インターバル (ミリ秒)
   * @param proc 処理本体
   */
  public static void execute(int retryCount, int interval, Runnable proc) {
    execute(retryCount, interval, proc, null);
  }
  
  /**
   * 指定された情報を元にリトライ処理付きでRunnableを実行します。<br>
   * 処理が試行される回数は、（一度目の実行 + リトライ回数）です。<br>
   * エラーコールバックを指定している場合、エラーが発生すると指定されたエラーコールバックが呼ばれます。<br>
   * エラーコールバックを指定していない場合で、エラーが発生すると呼び元に{@link RetryException}がスローされます。<br>
   * 
   * @param retryCount リトライ回数
   * @param interval インターバル (ミリ秒)
   * @param proc 処理本体
   * @param errorCallback エラーコールバック
   */
  public static void execute(int retryCount, int interval, Runnable proc, Retry.ErrorCallback errorCallback) {
    
    int activeRetryCount = 0;
    List<Throwable> exList = new ArrayList<Throwable>();
    
    boolean stopThrowException = false;
    try {
      
      for (int i = 0; i < (retryCount + 1); i++) {
        
        try {
          proc.run();
          
          activeRetryCount = (i + 1);
          stopThrowException = true;
          
          break;
        } catch (Throwable ex) {

          exList.add(ex);
          
          boolean isInitialProc = true;
          boolean retryStop = false;
          try {
            if (activeRetryCount != 0) {
              
              isInitialProc = false;
              
              if (errorCallback != null) {
                
                ErrorInfo info = new ErrorInfo(activeRetryCount, ex);
                errorCallback.invoke(info);
                
                if (info.isRetryStop()) {
                  retryStop = true;
                  break;
                }
              }
              
              if (activeRetryCount < retryCount) {
                try {
                  Thread.sleep(interval);
                } catch (InterruptedException e) {
                }
              }
            }
          } finally {
            if ((activeRetryCount <= retryCount) && isInitialProc || !retryStop) {
              activeRetryCount++;
            }
          }
        }
      }
    } finally {
      if (!stopThrowException) {
        if (!exList.isEmpty() && errorCallback == null) {
            throw new RetryException(exList);
        }
      }
    }
  }
  
  /**
   * エラーコールバックインターフェースです。<br>
   * 
   * @author devlights
   */
  public interface ErrorCallback {
    
    /**
     * 処理を実行します。<br>
     * リトライ処理を中断する場合は、{@link Retry.ErrorInfo#setRetryStop()}を<br>
     * 呼びだす事で中断できます。<br>
     * 
     * @param info エラー情報
     */
    void invoke(Retry.ErrorInfo info);
  }
  
  /**
   * エラー情報を表すクラスです。<br>
   * 
   * @author devlights
   */
  public static class ErrorInfo {
    
    /** 現在のリトライ回数 */
    private int _retryCount;
    /** 原因 */
    private Throwable _cause;
    /** リトライを中断するか否か */
    private boolean _theEnd;
    
    /**
     * コンストラクタ
     * 
     * @param retryCount 現在のリトライ回数
     * @param cause 原因
     */
    ErrorInfo(int retryCount, Throwable cause) {
      _retryCount = retryCount;
      _cause = cause;
      _theEnd = false;
    }
    
    /**
     * 現在のリトライ回数を取得します。<br>
     * 
     * @return 現在のリトライ回数
     */
    public int getCurrentRetryCount() {
      return _retryCount;
    }
    
    /**
     * 原因を取得します。<br>
     * 
     * @return 原因
     */
    public Throwable getCause() {
      return _cause;
    }
    
    /**
     * リトライが中断されているか否かを取得します。<br>
     * 
     * @return リトライを中断するか否か
     */
    boolean isRetryStop() {
      return _theEnd;
    }
    
    /**
     * リトライを中断します。<br>
     */
    public void setRetryStop() {
      _theEnd = true;
    }
  }
  
  /**
   * リトライが発生した際に内部エラー情報を保持し、呼び元に通知するための例外クラスです。<br>
   * 
   * @author devlights
   */
  public static class RetryException extends RuntimeException {
    
    /**
     * シリアルバージョンID
     */
    private static final long serialVersionUID = 5336348647566895333L;
    
    /** 例外リスト */
    private List<Throwable> _exList;
    
    /**
     * コンストラクタ
     * 
     * @param exList 例外リスト
     */
    public RetryException(List<Throwable> exList) {
      _exList = exList;
    }
    
    /**
     * 処理実行時に発生した例外のリストを取得します。<br>
     * 
     * @return 例外リスト
     */
    public List<Throwable> getExceptionList() {
      return _exList;
    }
  }
}
