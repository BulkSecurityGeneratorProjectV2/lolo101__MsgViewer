package at.redeye.FrameWork.widgets.calendar;

import at.redeye.FrameWork.utilities.calendar.AustrianHolidays;
import at.redeye.FrameWork.widgets.calendarday.CalendarDay;
import at.redeye.FrameWork.widgets.calendarday.DayEventListener;

public class TestFrame extends javax.swing.JFrame implements DayEventListener {

    private final CalendarComponent month = new CalendarComponent();

    public TestFrame() {
        initComponents();

        setContentPane( month );

        month.setHolidays(new AustrianHolidays());
        month.setMonth(6, 2009);
        month.setListener(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 805, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 590, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
    * @param args the command line arguments
    */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new TestFrame().setVisible(true));
    }

    public void onClicked(CalendarDay day) {
        System.out.println("Clicked on day " + month.isWhatDayOfMonth(day) );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
