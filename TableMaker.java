import java.util.*;
import java.io.*;

public class TableMaker {

    // テーブル初期設定行列
    private final static int table_set = 1;
    private final static int table_replace = 0;
    private final static int table_name = 1;

    // テーブル詳細設定開始行
    private final static int detail_row_start = 3;

    // テーブル詳細設定列番号
    private final static int field_set = 0;
    private final static int type_set = 1;
    private final static int null_set = 2;
    private final static int primary_set = 3;
    private final static int default_set = 4;
    private final static int unique_set = 5;
    private final static int auto_increment = 6;
    private final static int foreign_tbl = 7;
    private final static int foreign_col = 8;

    private final static ArrayList<String> col_type = new ArrayList<String>(Arrays.asList(
        "int",
        "integer",
        "smallint",
        "float",
        "real",
        "double",
        "doubleprecision",
        "number",
        "decimal"
    ));

    public static void main(String[] args) {

        if(args.length != 1){
            System.out.println("パラメータの数が違います");
            System.exit(1);
        }

        try {
            String path = System.getProperty("user.dir").replace("\\", "/");
            File inFile = new File(path + "/" + args[0]);
            if(!inFile.exists()) {
                System.out.println("File is not exist");
                return;
            }

            FileReader fileReader = new FileReader(inFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            ArrayList<ArrayList<String>> inlet_outer = new ArrayList<ArrayList<String>>();

            String data = new String();
            while ((data = bufferedReader.readLine()) != null) {
                ArrayList<String> inlet_inner = new ArrayList<String>(Arrays.asList(data.split(",")));
                inlet_outer.add(inlet_inner);
            }

            bufferedReader.close();
            fileReader.close();

            ArrayList<String> outlet_al = converter(inlet_outer);

            File outFile = new File(path + "/" + args[0].replace("csv", "sql"));
            FileWriter fileWriter = new FileWriter(outFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String str : outlet_al) {
                bufferedWriter.write(str);
            }

            bufferedWriter.close();
            fileWriter.close();

            System.out.println("変換に成功しました");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // csvの内容に合わせて変換する
    private static ArrayList<String> converter(ArrayList<ArrayList<String>> inlet_array) {

        ArrayList<String> outlet_array = new ArrayList<String>();
        ArrayList<String> primary_keys = new ArrayList<String>();
        ArrayList<String> foreign_keys = new ArrayList<String>();

        if (inlet_array.get(table_set).get(table_replace).toLowerCase().equals("yes")) {
            outlet_array.add("DROP TABLE IF EXISTS " + inlet_array.get(table_set).get(table_name));
        }

        outlet_array.add("CREATE TABLE " + inlet_array.get(table_set).get(table_name));

        for (int i=detail_row_start; i<inlet_array.size(); i++) {
            ArrayList<String> tmp = new ArrayList<String>();
            for (int j=0; j<inlet_array.get(i).size(); j++) {
                switch (j) {
                    case field_set:
                    case type_set:
                        tmp.add(inlet_array.get(i).get(j));
                        break;
                    case null_set:
                        if (inlet_array.get(i).get(j).toLowerCase().equals("no")) {
                            tmp.add("NOT NULL");
                        }
                        break;
                    case primary_set:
                        if (inlet_array.get(i).get(j).toLowerCase().equals("yes")) {
                            primary_keys.add(inlet_array.get(i).get(field_set));
                        }
                        break;
                    case default_set:
                        if (inlet_array.get(i).get(j).equals("\"\"")) {
                            // 何もしない
                        } else if (inlet_array.get(i).get(j).equals("")) {
                            // 何もしない
                        } else {
                            tmp.add(default_check(
                                inlet_array.get(i).get(type_set),
                                inlet_array.get(i).get(j)
                            ));
                        }
                        break;
                    case unique_set:
                        if(inlet_array.get(i).get(j).toLowerCase().equals("yes")) {
                            tmp.add("UNIQUE");
                        }
                        break;
                    case auto_increment:
                        if(inlet_array.get(i).get(j).toLowerCase().equals("yes")) {
                            tmp.add("AUTO_INCREMENT");
                        }
                        break;
                    case foreign_tbl:
                        if (!(inlet_array.get(i).get(j).equals("")) && inlet_array.get(i).size() > foreign_col) {
                            foreign_keys.add(foreign_converter(inlet_array.get(i)));
                        } else {
                            System.out.println("foreign key setting error 01");
                            System.exit(1);
                        }
                        break;
                    default:
                        break;
                }
            }
            outlet_array.add(String.join(" ", tmp).replaceAll(" +", " ").trim());
        }

        if (primary_keys.size() != 0) {
            outlet_array.add("PRIMARY KEY(" + String.join(",", primary_keys) + ")");
        }

        if (foreign_keys.size() != 0) {
            for (String f_key : foreign_keys) {
                outlet_array.add(f_key);
            }
        }

        // 行末の処理
        for (int i=0; i<outlet_array.size(); i++) {
            outlet_array.set(i, outlet_array.get(i) + (i + 1 == outlet_array.size() ? ";\r\n" : ",\r\n"));
        }

        return outlet_array;
    }


    // デフォルト設定があった場合に動作するメソッド
    private static String default_check(String type_str, String default_str) {
        String return_str = new String();
        Integer tmp;

        if (type_str.indexOf("(") != -1)  {
            tmp = type_str.indexOf("(");
        } else {
            tmp = type_str.length();
        }

        if (col_type.contains(type_str.substring(0, tmp).toLowerCase())) {
            return_str = default_str;
        } else {
            return_str = "'" + default_str + "'";
        }
        if (return_str == "''") {
            return_str = "";
        }

        return return_str;
    }


    private static String foreign_converter(ArrayList<String> f_array) {
        List<String> foreign_cols = f_array.subList(foreign_col, f_array.size());
        for (String item : foreign_cols) {
            if (item.replace("\"", "").equals("")) {
                System.out.println("foreign key setting error 02");
                System.exit(1);
            }
        }
        String conv_foreign_cols;
        if (foreign_cols.size() > 1) {
            conv_foreign_cols = String.join(", ", foreign_cols).replaceAll("\"", "");
        } else {
            conv_foreign_cols = foreign_cols.get(0);
        }
        return foreign_check(
            f_array.get(foreign_tbl),
            conv_foreign_cols,
            f_array.get(field_set)
        );
    }


    private static String foreign_check(String tbl_str, String col_str, String foreign_str) {
        if (tbl_str.equals("") || col_str.equals("")) {
            return "";
        } else {
            return "FOREIGN KEY " + foreign_str + " REFERENCES " + tbl_str + "(" + col_str + ")";
        }
    }
}