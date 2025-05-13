package com.github.weather_chart.service;

import com.github.weather_chart.dto.OpenMeteoDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WeatherChartService {

    private final WebClient webClient;

    public void download(HttpServletResponse response) throws IOException {
        Mono<OpenMeteoDto> openMeteoDtoMono = webClient.get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude=37.566&longitude=126.9784&hourly=temperature_2m&forecast_days=1")
                .retrieve()
                .bodyToMono(OpenMeteoDto.class);

        OpenMeteoDto openMeteoDto = openMeteoDtoMono.block();
        OpenMeteoDto.Hourly hourly = openMeteoDto.getHourly();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("data sheet");

            CreationHelper creationHelper = wb.getCreationHelper();
            CellStyle dateCellStyle = wb.createCellStyle();
            short dateFormat = creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm");
            dateCellStyle.setDataFormat(dateFormat);

            for (int i = 0; i < hourly.getTime().length; i++) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(hourly.getTime()[i]);
                dateCell.setCellStyle(dateCellStyle);
                row.createCell(1).setCellValue(hourly.getTemperature_2m()[i]);
            }

            // 차트 생성
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 1, 16, 20);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Weather Chart");

            XDDFDataSource<String> xDate = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                    new CellRangeAddress(0, hourly.getTime().length - 1, 0, 0));
            XDDFNumericalDataSource<Double> yData = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(0, hourly.getTime().length - 1, 1, 1));

            XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE,
                    chart.createCategoryAxis(AxisPosition.BOTTOM),
                    chart.createValueAxis(AxisPosition.LEFT));

            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(xDate, yData);

            // 라인 스타일
            XDDFSolidFillProperties fill = new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLUE));
            XDDFLineProperties line = new XDDFLineProperties();
            line.setFillProperties(fill);
            series.setShapeProperties(new XDDFShapeProperties());
            series.getShapeProperties().setLineProperties(line);

            // 마커 스타일
            series.setMarkerStyle(MarkerStyle.CIRCLE);
            CTLineSer ctLineSer = series.getCTLineSer();

            if (!ctLineSer.isSetMarker()) {
                ctLineSer.addNewMarker();
            }

            CTShapeProperties spPr = ctLineSer.getMarker().isSetSpPr()
                    ? ctLineSer.getMarker().getSpPr()
                    : ctLineSer.getMarker().addNewSpPr();

            CTSolidColorFillProperties solidFill = CTSolidColorFillProperties.Factory.newInstance();
            CTSRgbColor blue = CTSRgbColor.Factory.newInstance();
            blue.setVal(new byte[]{0, 0, (byte) 255}); // RGB (0,0,255)
            solidFill.setSrgbClr(blue);
            spPr.setSolidFill(solidFill);

            CTLineProperties ln = spPr.isSetLn() ? spPr.getLn() : spPr.addNewLn();
            CTSolidColorFillProperties lineFill = CTSolidColorFillProperties.Factory.newInstance();
            CTSRgbColor lineBlue = CTSRgbColor.Factory.newInstance();
            lineBlue.setVal(new byte[]{0, 0, (byte) 255});
            lineFill.setSrgbClr(lineBlue);
            ln.setSolidFill(lineFill);

            chart.plot(data);

            // 응답
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=weather_data.xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
