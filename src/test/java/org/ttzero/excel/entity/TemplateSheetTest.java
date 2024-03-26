/*
 * Copyright (c) 2017-2024, guanquan.wang@yandex.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.ttzero.excel.entity;

import org.junit.Test;
import org.ttzero.excel.entity.style.Font;
import org.ttzero.excel.entity.style.Horizontals;
import org.ttzero.excel.entity.style.Styles;
import org.ttzero.excel.reader.Cell;
import org.ttzero.excel.reader.Dimension;
import org.ttzero.excel.reader.ExcelReader;
import org.ttzero.excel.reader.FullSheet;
import org.ttzero.excel.reader.Row;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ttzero.excel.reader.ExcelReaderTest.testResourceRoot;

/**
 * @author guanquan.wang at 2024-01-25 09:57
 */
public class TemplateSheetTest extends WorkbookTest {

    @Test public void testSimpleTemplate() throws IOException {
        String fileName = "simple template sheets.xlsx";
        new Workbook()
            .addSheet(new TemplateSheet("模板 1.xlsx", testResourceRoot().resolve("1.xlsx"))) // <- 模板工作表
            .addSheet(new ListSheet<>("普通工作表", ListObjectSheetTest.Item.randomTestData())) // <- 普通工作表
            .addSheet(new TemplateSheet("模板 fracture merged.xlsx", testResourceRoot().resolve("fracture merged.xlsx"))) // <- 模板工作表
            .addSheet(new TemplateSheet("复制空白工作表", testResourceRoot().resolve("#81.xlsx"), "Sheet2")) // 空白工作表模板
            .writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            assertEquals(reader.getSheetCount(), 4);
            // TODO 判断每个工作表的内容和样式
        }
    }

    @Test public void testAllTemplateSheets() throws IOException {
        String fileName = "all template sheets.xlsx";
        Workbook workbook = new Workbook();
        File[] files = testResourceRoot().toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xlsx")) {
                    try (ExcelReader reader = ExcelReader.read(file.toPath())) {
                        org.ttzero.excel.reader.Sheet[] sheets = reader.all();
                        for (org.ttzero.excel.reader.Sheet sheet : sheets) {
                            workbook.addSheet(new TemplateSheet(file.getName() + "$" + sheet.getName(), file.toPath(), sheet.getName()));
                        }
                    }
                }
            }
        }
        workbook.writeTo(getOutputTestPath().resolve(fileName));
    }

    @Test public void testTemplate() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("name", author);
        map.put("score", random.nextInt(90) + 10);
        map.put("date", LocalDate.now().toString());
        map.put("desc", "暑假");

        new Workbook()
            .addSheet(new TemplateSheet(Files.newInputStream(testResourceRoot().resolve("template.xlsx")))
                .setData(map))
            .writeTo(defaultTestPath.resolve("fill inputstream template with map.xlsx"));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve("fill inputstream template with map.xlsx"))) {
            for (Iterator<Row> it = reader.sheet(0).iterator(); it.hasNext(); ) {
                Row row = it.next();
                switch (row.getRowNum()) {
                    case 1:
                        assertEquals("通知书", row.getString(0).trim());
                        break;
                    case 3:
                        assertEquals((map.get("name") + " 同学，在本次期末考试的成绩是 " + map.get("score")+ "，希望"), row.getString(1).trim());
                        break;
                    case 4:
                        assertEquals(("下学期继续努力，祝你有一个愉快的" + map.get("desc") + "。"), row.getString(0).trim());
                        break;
                    case 23:
                        assertEquals(map.get("date"), row.getString(0).trim());
                        break;
                    default:
                        assertTrue(row.isBlank());
                }
            }
        }
    }

    @Test public void testFillObject() throws IOException {
        final String fileName = "fill object.xlsx";
        YzEntity yzEntity = YzEntity.mock();
        YzOrderEntity yzOrderEntity = YzOrderEntity.mock();
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                .setData(yzEntity)
                .setData("YzEntity", yzOrderEntity)
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            FullSheet sheet = reader.sheet(0).asFullSheet();
            Styles styles = reader.getStyles();
            Iterator<org.ttzero.excel.reader.Row> iter = sheet.iterator();
            // 第一行
            assertTrue(iter.hasNext());
            org.ttzero.excel.reader.Row row = iter.next();
            assertEquals(row.getString(0), yzEntity.gsName + "精品采购订单");
            Font font = styles.getFont(row.getCellStyle(0));
            assertEquals(font.getName(), "Calibri");
            assertEquals(font.getSize(), 20);
            assertTrue(font.isBold());

            // 第二行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(yzEntity.gysName, row.getString(3));
            assertEquals(yzEntity.orderNo, row.getString(11));

            // 第三行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(yzEntity.gsName, row.getString(3));
            assertEquals(yzEntity.orderStatus, row.getString(11));

            // 第四行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(yzEntity.jsName, row.getString(3));
            assertTrue(yzEntity.cgDate.getTime() - row.getDate(11).getTime() < 1000); // 导出时Excel的日期丢失了毫秒值

            // 第五行
            assertTrue(iter.hasNext());
            assertTrue(iter.next().isBlank()); // 空行

            // 第六行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(row.getFirstColumnIndex(), 0);
            final String[] titles = {"序号", "精品代码", null, "精品名称", null, null, "数量", "不含税单价", "不含税金额", "税率", "含税单价", "含税金额", "备注"};
            for (int i = 0, len = Math.min(row.getLastColumnIndex(), titles.length); i < len; i++) {
                Cell cell = row.getCell(i);
                assertEquals(titles[i], row.getString(cell));
                int style = row.getCellStyle(cell);
                font = styles.getFont(style);
                assertTrue(font.isBold());
                assertEquals(font.getName(), "Calibri");
                assertEquals(font.getSize(), 11);
                assertEquals(styles.getHorizontal(style), Horizontals.CENTER);
            }

            // 第七行
            assertTrue(iter.hasNext());
            assertTrue(iter.next().isBlank()); // 空行

            // 第八行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(row.getFirstColumnIndex(), 0);
            assertEquals(row.getInt(0).intValue(), 1);
            assertEquals(row.getString(1), yzOrderEntity.jpCode);
            assertEquals(row.getString(3), yzOrderEntity.jpName);
            assertEquals(row.getInt(6).intValue(), yzOrderEntity.num);
            assertTrue(Math.abs(row.getDouble(7) - yzOrderEntity.price) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(8) - yzOrderEntity.amount) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(9) - yzOrderEntity.tax) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(10) - yzOrderEntity.taxPrice) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(11) - yzOrderEntity.taxAmount) <= 0.00001);
            assertEquals(row.getString(12), yzOrderEntity.remark);

            // 第九行
            assertTrue(iter.hasNext());
            assertTrue(iter.next().isBlank()); // 空行
            // 第十行
            assertTrue(iter.hasNext());
            assertTrue(iter.next().isBlank()); // 空行

            // 第十一行
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(row.getString(0), "合计");
            assertEquals(row.getInt(6).intValue(), yzEntity.nums);
            assertTrue(Math.abs(row.getDouble(7) - yzEntity.priceTotal) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(8) - yzEntity.amountTotal) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(9) - yzEntity.taxTotal) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(10) - yzEntity.taxPriceTotal) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(11) - yzEntity.taxAmountTotal) <= 0.00001);
        }
    }

    @Test public void testFillMap() throws IOException {
        final String fileName = "fill map.xlsx";
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                    .setData(YzEntity.mock())
                    // 单Map测试
                    .setData("YzEntity", new HashMap<String, Object>() {{
                        put("xh", 1);
                        put("jpCode", "code1");
                        put("jpName", "name1");
                        put("num", 10);
                        put("price", 10);
                        put("amount", 100);
                        put("tax", 0.6);
                        put("taxPrice", 11.6);
                        put("taxAmount", 116);
                        put("remark", "很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长");
                    }})
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            Iterator<org.ttzero.excel.reader.Row> iter = reader.sheet(0).iterator();
            // 跳过前7行
            iter.next();iter.next(); iter.next();iter.next();iter.next();iter.next();iter.next();

            org.ttzero.excel.reader.Row row = iter.next();
            assertEquals(row.getString(12), "很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长");
        }
    }

    @Test public void testFillListObject() throws IOException {
        final String fileName = "fill list object.xlsx";
        List<YzOrderEntity> expectList = YzOrderEntity.randomData();
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                    .setData(YzEntity.mock())
                    .setData("YzEntity", expectList)
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            assertListObject(reader.sheet(0).asFullSheet(), expectList);
        }
    }

    @Test public void testFillListMap() throws IOException {
        final String fileName = "fill list map.xlsx";
        List<Map<String, Object>> expectList = YzOrderEntity.randomMap();
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                .setData(YzEntity.mock())
                .setData("YzEntity", expectList)
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            assertListMap(reader.sheet(0).asFullSheet(), expectList);
        }
    }

    @Test public void testFillSupplierListObject() throws IOException {
        final String fileName = "fill supplier list object.xlsx";
        List<YzOrderEntity> expectList = new ArrayList<>();
        int[] page = { 0 };
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                .setData(YzEntity.mock())
                .setData("YzEntity", () -> {
                    List<YzOrderEntity> sub = null;
                    // 拉取10页数据
                    if (page[0]++ <= 10) {
                        sub = YzOrderEntity.randomData();
                        expectList.addAll(sub);
                    }
                    return sub;
                })
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            assertListObject(reader.sheet(0).asFullSheet(), expectList);
        }
    }

    @Test public void testFillSupplierListMap() throws IOException {
        final String fileName = "fill supplier list map.xlsx";
        List<Map<String, Object>> expectList = new ArrayList<>();
        int[] page = { 0 };
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"))
                .setData(YzEntity.mock())
                .setData("YzEntity", () -> {
                    List<Map<String, Object>> sub = null;
                    // 拉取10页数据
                    if (page[0]++ <= 10) {
                        sub = YzOrderEntity.randomMap();
                        expectList.addAll(sub);
                    }
                    return sub;
                })
            ).writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            assertListMap(reader.sheet(0).asFullSheet(), expectList);
        }
    }

    @Test public void testEmptyNamespace() throws IOException {
        final String fileName = "fill empty namespace.xlsx";
        List<YzOrderEntity> yzOrderEntity = YzOrderEntity.randomData();
        new Workbook()
            .addSheet(new TemplateSheet(testResourceRoot().resolve("template2.xlsx"), "Sheet2").setPrefix("{")
                .setData("", yzOrderEntity))
            .writeTo(defaultTestPath.resolve(fileName));

        try (ExcelReader reader = ExcelReader.read(defaultTestPath.resolve(fileName))) {
            FullSheet sheet = reader.sheet(0).asFullSheet();
            List<Dimension> mergeCells = sheet.getMergeCells();
            Map<Long, Dimension> mergeCellMap = new HashMap<>(mergeCells.size());
            for (Dimension dim : mergeCells) mergeCellMap.put(TemplateSheet.dimensionKey(dim), dim);
            int i = 0;
            for (Iterator<Row> iter = sheet.header(1, 2).iterator(); i < yzOrderEntity.size() && iter.hasNext(); i++) {
                Row row = iter.next();
                YzOrderEntity expect = yzOrderEntity.get(i);
                assertEquals(row.getInt(0).intValue(), expect.xh);
                assertEquals(row.getString(1), "中文" + expect.jpCode + "No." + expect.xh + "追加内容" + expect.num);
                assertEquals(row.getString(3), expect.jpName);
                assertEquals(row.getInt(6).intValue(), expect.num);
                assertTrue(Math.abs(row.getDouble(7) - expect.price) <= 0.00001);
                assertTrue(Math.abs(row.getDouble(8) - expect.amount) <= 0.00001);
                assertTrue(Math.abs(row.getDouble(9) - expect.tax) <= 0.00001);
                assertTrue(Math.abs(row.getDouble(10) - expect.taxPrice) <= 0.00001);
                assertTrue(Math.abs(row.getDouble(11) - expect.taxAmount) <= 0.00001);
                assertEquals(row.getString(12), expect.remark);

                // 判断是否带合并
                Dimension mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 1));
                assertNotNull(mergeCell);
                assertEquals(mergeCell.width, 2);
                mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 3));
                assertNotNull(mergeCell);
                assertEquals(mergeCell.width, 3);
                mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 12));
                assertNotNull(mergeCell);
                assertEquals(mergeCell.width, 5);
            }
        }
    }

    static void assertListObject(FullSheet sheet, List<YzOrderEntity> expectList) {
        Iterator<org.ttzero.excel.reader.Row> iter = sheet.header(6, 7).iterator();

        List<Dimension> mergeCells = sheet.getMergeCells();
        assertEquals(mergeCells.size(), 26 + expectList.size() * 3);
        Map<Long, Dimension> mergeCellMap = new HashMap<>(mergeCells.size());
        for (Dimension dim : mergeCells) {
            mergeCellMap.put(TemplateSheet.dimensionKey(dim), dim);
        }
        org.ttzero.excel.reader.Row row;
        for (YzOrderEntity expect : expectList) {
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(row.getFirstColumnIndex(), 0);
            assertEquals(row.getInt(0).intValue(), expect.xh);
            assertEquals(row.getString(1), expect.jpCode);
            assertEquals(row.getString(3), expect.jpName);
            assertEquals(row.getInt(6).intValue(), expect.num);
            assertTrue(Math.abs(row.getDouble(7) - expect.price) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(8) - expect.amount) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(9) - expect.tax) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(10) - expect.taxPrice) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(11) - expect.taxAmount) <= 0.00001);
            assertEquals(row.getString(12), expect.remark);

            // 判断是否带合并
            Dimension mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 1));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 2);
            mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 3));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 3);
            mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 12));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 5);
        }
        // 跳过2行
        assertTrue(iter.next().isBlank());
        assertTrue(iter.next().isBlank());

        // 合计行
        row = iter.next();
        assertEquals(row.getString(0), "合计");
        Dimension mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 0));
        assertNotNull(mergeCell);
        assertEquals(mergeCell.width, 6);
        mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 12));
        assertNotNull(mergeCell);
        assertEquals(mergeCell.width, 5);
    }

    static void assertListMap(FullSheet sheet, List<Map<String, Object>> expectList) {
        Iterator<org.ttzero.excel.reader.Row> iter = sheet.header(6, 7).iterator();

        List<Dimension> mergeCells = sheet.getMergeCells();
        assertEquals(mergeCells.size(), 26 + expectList.size() * 3);
        Map<Long, Dimension> mergeCellMap = new HashMap<>(mergeCells.size());
        for (Dimension dim : mergeCells) {
            mergeCellMap.put(TemplateSheet.dimensionKey(dim), dim);
        }
        org.ttzero.excel.reader.Row row;
        for (Map<String, Object> expect : expectList) {
            assertTrue(iter.hasNext());
            row = iter.next();
            assertEquals(row.getFirstColumnIndex(), 0);
            assertEquals(row.getInt(0), expect.get("xh"));
            assertEquals(row.getString(1), expect.get("jpCode"));
            assertEquals(row.getString(3), expect.get("jpName"));
            assertEquals(row.getInt(6), expect.get("num"));
            assertTrue(Math.abs(row.getDouble(7) - (double) expect.get("price")) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(8) - (double) expect.get("amount")) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(9) - (double) expect.get("tax")) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(10) - (double) expect.get("taxPrice")) <= 0.00001);
            assertTrue(Math.abs(row.getDouble(11) - (double) expect.get("taxAmount")) <= 0.00001);
            assertEquals(row.getString(12), expect.get("remark"));

            // 判断是否带合并
            Dimension mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 1));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 2);
            mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 3));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 3);
            mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 12));
            assertNotNull(mergeCell);
            assertEquals(mergeCell.width, 5);
        }
        // 跳过2行
        assertTrue(iter.next().isBlank());
        assertTrue(iter.next().isBlank());

        // 合计行
        row = iter.next();
        assertEquals(row.getString(0), "合计");
        Dimension mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 0));
        assertNotNull(mergeCell);
        assertEquals(mergeCell.width, 6);
        mergeCell = mergeCellMap.get(TemplateSheet.dimensionKey(row.getRowNum() - 1, 12));
        assertNotNull(mergeCell);
        assertEquals(mergeCell.width, 5);
    }

    public static class YzEntity {
        private String gysName;
        private String gsName;
        private String jsName;
        private String orderNo;
        private String orderStatus;
        private Date cgDate;
        private int nums;
        private double priceTotal;
        private double amountTotal;
        private double taxTotal;
        private double taxPriceTotal;
        private double taxAmountTotal;

        private static YzEntity mock() {
            YzEntity e = new YzEntity();
            e.gysName =" 供应商";
            e.gsName = "ABC公司";
            e.jsName = "亚瑟";
            e.cgDate = new Date();
            e.orderNo = "JD-0001";
            e.orderStatus = "OK";
            e.nums = 10;
            e.priceTotal = 10;
            e.amountTotal = 10;
            e.taxTotal = 10;
            e.taxPriceTotal = 10;
            e.taxAmountTotal = 10;
            return e;
        }
    }

    public static class YzOrderEntity {
        private int xh;
        private String jpCode;
        private String jpName;
        private int num;
        private double price;
        private double amount;
        private double tax;
        private double taxPrice;
        private double taxAmount;
        private String remark;

        private static YzOrderEntity mock() {
            YzOrderEntity e = new YzOrderEntity();
            e.xh = 1;
            e.jpCode = "code1";
            e.jpName = "name1";
            e.num = e.xh;
            e.price = 3.5D * e.xh;
            e.amount = e.price * e.num;
            e.tax = 0.006;
            e.taxPrice = e.price * (e.tax + 1);
            e.taxAmount = e.amount * e.tax;
            e.remark = "备注";
            return e;
        }

        private static List<YzOrderEntity> randomData() {
            List<YzOrderEntity> list = new ArrayList<>(10);
            for (int i = 0; i < 10; i++) {
                YzOrderEntity e = new YzOrderEntity();
                e.xh = i + 1;
                e.jpCode = "code" + e.xh;
                e.jpName = "name" + e.xh;
                e.num = e.xh;
                e.price = 3.5D * e.xh;
                e.amount = e.price * e.num;
                e.tax = 0.006;
                e.taxPrice = e.price * (e.tax + 1);
                e.taxAmount = e.amount * e.tax;
                e.remark = "备注" + e.xh;
                list.add(e);
            }
            return list;
        }
        private static List<Map<String, Object>> randomMap() {
            List<Map<String, Object>> list = new ArrayList<>(10);
            for (int i = 0, j; i < 10; i++) {
                Map<String, Object> map = new HashMap<>();
                map.put("xh", (j = i + 1));
                map.put("jpCode", "code" + j);
                map.put("jpName", "name" + j);
                map.put("num", j);
                map.put("price", 3.5D * j);
                map.put("amount", 3.5D * j * j);
                map.put("tax", 0.006);
                map.put("taxPrice", 3.5D * j * 1.006);
                map.put("taxAmount", 3.5D * j * 1.006 * j);
                map.put("remark", "备注" + j);
                list.add(map);
            }
            return list;
        }
    }

}
