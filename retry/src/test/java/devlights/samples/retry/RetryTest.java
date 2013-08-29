package devlights.samples.retry;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import devlights.samples.retry.Retry.ErrorInfo;
import devlights.samples.retry.Retry.RetryException;

public class RetryTest {

  @Test
  public void エラーが無い状態だと一回だけ実行される() {
    
    // Arrange
    int retryCount = 3;
    int interval = 500;
    
    // Act
    final List<Boolean> tmpList = new ArrayList<Boolean>();
    Retry.execute(retryCount, interval, new Runnable() {
      public void run() {
        tmpList.add(true);
      }
    });
    
    // Assert
    assertThat(tmpList.size(), is(1));
  }

  @Test
  public void エラーが発生した場合指定された回数分リトライ処理を行う() {
    
    // Arrange
    int retryCount = 3;
    int interval = 100;
    
    // Act
    final List<Boolean> tmpList = new ArrayList<Boolean>();
    
    try {
      Retry.execute(retryCount, interval, new Runnable() {
        public void run() {
          tmpList.add(true);
          throw new RuntimeException();
        }
      });
    } catch (RetryException ex) {
      // Assert
      assertThat(ex.getExceptionList().size(), is(4));
    }
    
    // Assert
    assertThat(tmpList.size(), is(4));
  }
  
  @Test
  public void エラーコールバックを指定するとエラー時に呼ばれる() {
    
    // Arrange
    int retryCount = 3;
    int interval = 100;
    
    // Act
    final List<Boolean> tmpList = new ArrayList<Boolean>();
    
    Retry.execute(retryCount, interval, new Runnable() {
      public void run() {
        tmpList.add(true);
        throw new RuntimeException();
      }
    }, new Retry.ErrorCallback() {
      int _count = 0;
      
      public void invoke(ErrorInfo info) {
        // Assert
        assertThat(info.getCurrentRetryCount(), is(++_count));
        assertThat(info.getCause(), not(nullValue()));
      }
    });
    
    // Assert
    assertThat(tmpList.size(), is(4));
  }
  
  @Test
  public void エラーコールバック内でリトライ停止設定するとリトライが中断される() {

    // Arrange
    int retryCount = 3;
    int interval = 100;
    
    // Act
    final List<Boolean> tmpList = new ArrayList<Boolean>();
    
    Retry.execute(retryCount, interval, new Runnable() {
      public void run() {
        tmpList.add(true);
        throw new RuntimeException();
      }
    }, new Retry.ErrorCallback() {
      int _count = 0;
      
      public void invoke(ErrorInfo info) {
        // Assert
        assertThat(info.getCurrentRetryCount(), is(++_count));
        assertThat(info.getCause(), not(nullValue()));
        
        if (_count == 2) {
          info.setRetryStop();
          return;
        }
      }
    });
    
    // Assert
    assertThat(tmpList.size(), is(3));
  }

}
