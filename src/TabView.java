// TabView.java
import javax.swing.*;

public interface TabView {
    String getTitle();      // 상단바 제목 동기화용
    void refresh();         // 탭 보여줄 때 데이터 갱신
    JComponent getComponent();
}

