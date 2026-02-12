import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;
import java.awt.Desktop;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class StudentGradeTrackerGUI extends JFrame {
    CardLayout layout = new CardLayout();
    JPanel container = new JPanel(layout);

    DefaultTableModel model;
    JTable table;
    JLabel statsLabel;


    public StudentGradeTrackerGUI() {

        setTitle("Student Grade Tracker System");
        setSize(1200,750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        createTables();
        System.out.println(new File("students.db").getAbsolutePath());

        container.add(loginPage(),"LOGIN");
        container.add(signupPage(),"SIGNUP");
        container.add(dashboard(),"HOME");
        container.add(addPage(),"ADD");

        add(container);
        layout.show(container,"LOGIN");

        setVisible(true);
    }

    // ================= DATABASE =================
    private void createTables(){
        try(Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                password TEXT);
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                roll INTEGER UNIQUE,
                math INTEGER,
                science INTEGER,
                computer INTEGER);
            """);

        } catch(Exception e){ e.printStackTrace(); }
    }

    // ================= UI HELPERS =================
    private void styleButton(JButton b, Color c){
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new java.awt.Font("Segoe UI",java.awt.Font.BOLD,14));
    }

    private JPanel header(String msg){
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(4, 16, 108));



        JLabel title = new JLabel("STUDENT GRADE TRACKER",SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI",java.awt.Font.BOLD,28));

        JLabel sub = new JLabel(msg,SwingConstants.CENTER);
        sub.setForeground(Color.WHITE);

        p.add(title,BorderLayout.CENTER);
        p.add(sub,BorderLayout.SOUTH);
        p.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
        return p;
    }

    private JPanel wrap(JPanel center,String msg){
        JPanel p = new JPanel(new BorderLayout());
        p.add(header(msg),BorderLayout.NORTH);
        p.add(center,BorderLayout.CENTER);
        return p;
    }

    // ================= LOGIN =================
    private JPanel loginPage(){

        JPanel form = new JPanel(new GridLayout(4,2,15,15));
        form.setBorder(BorderFactory.createEmptyBorder(200,400,200,400));

        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton login = new JButton("Login");
        JButton signup = new JButton("Signup");

        styleButton(login,new Color(0,150,0));
        styleButton(signup,new Color(0,90,200));

        form.add(new JLabel("Username:")); form.add(user);
        form.add(new JLabel("Password:")); form.add(pass);
        form.add(login); form.add(signup);

        login.addActionListener(e->{
            try(Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT * FROM users WHERE username=? AND password=?")){

                ps.setString(1,user.getText());
                ps.setString(2,String.valueOf(pass.getPassword()));
                ResultSet rs = ps.executeQuery();

                if(rs.next()){
                    layout.show(container,"HOME");
                } else {
                    JOptionPane.showMessageDialog(this,"Invalid Login");
                }

            } catch(Exception ex){ ex.printStackTrace(); }
        });

        signup.addActionListener(e->layout.show(container,"SIGNUP"));

        return wrap(form,"Welcome! Please Login");
    }

    // ================= SIGNUP =================
    private JPanel signupPage(){

        JPanel form = new JPanel(new GridLayout(4,2,15,15));
        form.setBorder(BorderFactory.createEmptyBorder(200,400,200,400));

        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton create = new JButton("Create Account");
        JButton back = new JButton("Back");

        styleButton(create,new Color(0,90,200));
        styleButton(back,new Color(84, 85, 87));

        form.add(new JLabel("Create Username:")); form.add(user);
        form.add(new JLabel("Create Password:")); form.add(pass);
        form.add(create); form.add(back);

        create.addActionListener(e->{
            try(Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users(username,password) VALUES(?,?)")){

                ps.setString(1,user.getText());
                ps.setString(2,String.valueOf(pass.getPassword()));
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this,"Signup Successful");
                layout.show(container,"LOGIN");

            } catch(Exception ex){
                JOptionPane.showMessageDialog(this,"User already exists");
            }
        });

        back.addActionListener(e->layout.show(container,"LOGIN"));

        return wrap(form,"Create Your Account");
    }

    // ================= DASHBOARD =================
    private JPanel dashboard(){

        JPanel panel = new JPanel(new GridLayout(7,1,15,15));
        panel.setBorder(BorderFactory.createEmptyBorder(120,400,120,400));

        statsLabel = new JLabel("",SwingConstants.CENTER);
        statsLabel.setFont(new java.awt.Font("Segoe UI",Font.PLAIN,16));

        JButton add = new JButton("Add Student");
        JButton view = new JButton("View Students");
        JButton topper = new JButton("Find Topper");
        JButton pass = new JButton("Pass Percentage");
        JButton logout = new JButton("Logout");

        styleButton(logout,new Color(200,0,0));
        styleButton(add,new Color(47, 170, 7));
        styleButton(view,new Color(47, 170, 7));
        styleButton(topper,new Color(47, 170, 7));
        styleButton(pass,new Color(47, 170, 7));

        panel.add(statsLabel);
        panel.add(add);
        panel.add(view);
        panel.add(topper);
        panel.add(pass);
        panel.add(logout);

        add.addActionListener(e->layout.show(container,"ADD"));
        view.addActionListener(e->openReport());
        topper.addActionListener(e->showTopper());
        pass.addActionListener(e->showPass());
        logout.addActionListener(e->layout.show(container,"LOGIN"));

        updateStats( );

        return wrap(panel,"Dashboard");
    }

    private void updateStats(){

        try(Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement()){

            ResultSet total = stmt.executeQuery(
                    "SELECT COUNT(*) as t FROM students");
            int t = total.getInt("t");

            ResultSet pass = stmt.executeQuery(
                    "SELECT COUNT(*) as p FROM students WHERE (math+science+computer)/3.0 >= 50");
            int p = pass.getInt("p");

            statsLabel.setText("Total Students: " + t +
                    " | Passed: " + p +
                    " | Failed: " + (t-p));

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // ================= ADD PAGE =================
    private JPanel addPage(){

        JPanel form = new JPanel(new GridLayout(6,2,15,15));
        form.setBorder(BorderFactory.createEmptyBorder(120,350,120,350));

        JTextField name = new JTextField();
        JTextField roll = new JTextField();
        JTextField math = new JTextField();
        JTextField science = new JTextField();
        JTextField computer = new JTextField();

        JButton save = new JButton("Save");
        JButton back = new JButton("Back");
        styleButton(save,new Color(40, 195, 11));
        styleButton(back,new Color(84, 85, 87));

        form.add(new JLabel("Name:")); form.add(name);
        form.add(new JLabel("Roll:")); form.add(roll);
        form.add(new JLabel("Math:")); form.add(math);
        form.add(new JLabel("Science:")); form.add(science);
        form.add(new JLabel("Computer:")); form.add(computer);
        form.add(save); form.add(back);

        save.addActionListener(e->{
            try{

                if(name.getText().trim().isEmpty()){
                    JOptionPane.showMessageDialog(this,"Name cannot be empty");
                    return;
                }

                int r = Integer.parseInt(roll.getText());
                int m = Integer.parseInt(math.getText());
                int s = Integer.parseInt(science.getText());
                int c = Integer.parseInt(computer.getText());

                if(m<0||m>100||s<0||s>100||c<0||c>100){
                    JOptionPane.showMessageDialog(this,"Marks must be 0-100");
                    return;
                }

                try(Connection con = DBConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO students(name,roll,math,science,computer) VALUES(?,?,?,?,?)")){

                    ps.setString(1,name.getText());
                    ps.setInt(2,r);
                    ps.setInt(3,m);
                    ps.setInt(4,s);
                    ps.setInt(5,c);
                    ps.executeUpdate();

                    JOptionPane.showMessageDialog(this,"Student Added");
                    layout.show(container,"HOME");
                }

            }catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Invalid input or Roll already exists");
            }
        });

        back.addActionListener(e->layout.show(container,"HOME"));

        return wrap(form,"Add Student Details");
    }

    // ================= REPORT PAGE =================
    private void openReport(){

        model = new DefaultTableModel(
                new String[]{"ID","Name","Roll","Math","Science","Computer","Average","Grade"},0);
        table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);

        JTextField search = new JTextField(15);
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton pdfOne = new JButton("Download Selected");
        JButton pdfAll = new JButton("Download Full Report");
        JButton back = new JButton("Back");
        styleButton(update,new Color(109, 220, 88));
        styleButton(delete,new Color(239, 48, 77));
        styleButton(pdfOne,new Color(3, 23, 177));
        styleButton(pdfAll,new Color(3, 23, 177));
        styleButton(back,new Color(84, 85, 87));

        JPanel bottom = new JPanel();
        bottom.add(new JLabel("Search:"));
        bottom.add(search);
        bottom.add(update);
        bottom.add(delete);
        bottom.add(pdfOne);
        bottom.add(pdfAll);
        bottom.add(back);

        search.addKeyListener(new KeyAdapter(){
            public void keyReleased(KeyEvent e){
                TableRowSorter<DefaultTableModel> sorter =
                        new TableRowSorter<>(model);
                table.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter(search.getText()));
            }
        });

        update.addActionListener(e->updateStudent());
        delete.addActionListener(e->deleteStudent());
        pdfOne.addActionListener(e->downloadSingle());
        pdfAll.addActionListener(e->downloadAll());
        back.addActionListener(e->layout.show(container,"HOME"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(header("Student Records"),BorderLayout.NORTH);
        panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.add(bottom,BorderLayout.SOUTH);

        container.add(panel,"REPORT");
        layout.show(container,"REPORT");

        refreshTable();
    }

    // ================= CRUD =================
    private void refreshTable(){
        model.setRowCount(0);
        try(Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM students")){

            while(rs.next()){
                int id=rs.getInt("id");
                String name=rs.getString("name");
                int roll=rs.getInt("roll");
                int math=rs.getInt("math");
                int science=rs.getInt("science");
                int computer=rs.getInt("computer");

                double avg=(math+science+computer)/3.0;
                String grade=avg>=85?"A":avg>=70?"B":avg>=50?"C":"Fail";

                model.addRow(new Object[]{
                        id,name,roll,math,science,computer,
                        String.format("%.2f",avg),grade});
            }

        } catch(Exception e){ e.printStackTrace(); }
    }

    private void updateStudent(){
        int row = table.getSelectedRow();
        if(row<0) return;

        int id = (int)table.getValueAt(row,0);

        JTextField name = new JTextField(table.getValueAt(row,1).toString());
        JTextField roll = new JTextField(table.getValueAt(row,2).toString());
        JTextField math = new JTextField(table.getValueAt(row,3).toString());
        JTextField science = new JTextField(table.getValueAt(row,4).toString());
        JTextField computer = new JTextField(table.getValueAt(row,5).toString());

        JPanel panel = new JPanel(new GridLayout(5,2));
        panel.add(new JLabel("Name:")); panel.add(name);
        panel.add(new JLabel("Roll:")); panel.add(roll);
        panel.add(new JLabel("Math:")); panel.add(math);
        panel.add(new JLabel("Science:")); panel.add(science);
        panel.add(new JLabel("Computer:")); panel.add(computer);

        int result = JOptionPane.showConfirmDialog(this,panel,
                "Update Student",JOptionPane.OK_CANCEL_OPTION);

        if(result==JOptionPane.OK_OPTION){
            try(Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE students SET name=?,roll=?,math=?,science=?,computer=? WHERE id=?")){

                ps.setString(1,name.getText());
                ps.setInt(2,Integer.parseInt(roll.getText()));
                ps.setInt(3,Integer.parseInt(math.getText()));
                ps.setInt(4,Integer.parseInt(science.getText()));
                ps.setInt(5,Integer.parseInt(computer.getText()));
                ps.setInt(6,id);
                ps.executeUpdate();
                refreshTable();

            } catch(Exception e){ e.printStackTrace(); }
        }
    }

    private void deleteStudent(){
        int row = table.getSelectedRow();
        if(row<0) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,"Are you sure you want to delete?",
                "Confirm Delete",JOptionPane.YES_NO_OPTION);

        if(confirm!=JOptionPane.YES_OPTION) return;

        int id = (int)table.getValueAt(row,0);

        try(Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM students WHERE id=?")){

            ps.setInt(1,id);
            ps.executeUpdate();
            refreshTable();

        } catch(Exception e){ e.printStackTrace(); }
    }

    private void showTopper(){
        try(Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT name,(math+science+computer)/3.0 AS avg FROM students ORDER BY avg DESC LIMIT 1")){

            if(rs.next())
                JOptionPane.showMessageDialog(this,
                        "Topper: "+rs.getString("name")+
                                "\nAverage: "+rs.getDouble("avg"));

        } catch(Exception e){ e.printStackTrace(); }
    }

    private void showPass(){
        try(Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement()){

            ResultSet total = stmt.executeQuery("SELECT COUNT(*) as t FROM students");
            int t = total.getInt("t");

            ResultSet pass = stmt.executeQuery(
                    "SELECT COUNT(*) as p FROM students WHERE (math+science+computer)/3.0 >= 50");
            int p = pass.getInt("p");

            if(t>0)
                JOptionPane.showMessageDialog(this,
                        "Overall Pass Percentage: "+(p*100.0/t)+"%");

        } catch(Exception e){ e.printStackTrace(); }
    }

    private void downloadSingle(){

        int row = table.getSelectedRow();
        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select a student first.");
            return;
        }

        try{
            String path = System.getProperty("user.home") + "/Downloads/Student_Report.pdf";

            Document doc = new Document();
            PdfWriter.getInstance(doc,new FileOutputStream(path));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18,
                            com.itextpdf.text.Font.BOLD);

            Paragraph title = new Paragraph("STUDENT REPORT\n\n",titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            com.itextpdf.text.pdf.PdfPTable pdfTable =
                    new com.itextpdf.text.pdf.PdfPTable(7);

            pdfTable.setWidthPercentage(100);

            // Headers
            for(int i=1;i<=7;i++){
                pdfTable.addCell(table.getColumnName(i));
            }

            // Data
            for(int i=1;i<=7;i++){
                pdfTable.addCell(table.getValueAt(row,i).toString());
            }

            doc.add(pdfTable);
            doc.close();

            Desktop.getDesktop().open(new File(path));
            JOptionPane.showMessageDialog(this,"PDF Downloaded Successfully");

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    private void downloadAll(){

        if(table.getRowCount() == 0){
            JOptionPane.showMessageDialog(this,"No data available.");
            return;
        }

        try{
            String path = System.getProperty("user.home") + "/Downloads/Full_Student_Report.pdf";

            Document doc = new Document();
            PdfWriter.getInstance(doc,new FileOutputStream(path));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18,
                            com.itextpdf.text.Font.BOLD);

            Paragraph title = new Paragraph("STUDENT REPORT\n\n",titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            com.itextpdf.text.pdf.PdfPTable pdfTable =
                    new com.itextpdf.text.pdf.PdfPTable(7);

            pdfTable.setWidthPercentage(100);

            // Header row
            for(int i=1;i<=7;i++){
                pdfTable.addCell(table.getColumnName(i));
            }

            // All data rows
            for(int row=0; row<table.getRowCount(); row++){
                for(int col=1; col<=7; col++){
                    pdfTable.addCell(table.getValueAt(row,col).toString());
                }
            }

            doc.add(pdfTable);
            doc.close();

            Desktop.getDesktop().open(new File(path));
            JOptionPane.showMessageDialog(this,"Full Report Downloaded Successfully");

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new StudentGradeTrackerGUI();
    }
}
