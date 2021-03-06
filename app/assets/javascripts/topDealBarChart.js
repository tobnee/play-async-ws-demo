function initChart(city) {
    var margin = {top: 20, right: 20, bottom: 30, left: 40},
        width = 960 - margin.left - margin.right,
        height = 500 - margin.top - margin.bottom;

    var formatPercent = d3.format(".0%");

    var x = d3.scale.ordinal()
        .rangeRoundBands([0, width], .1, 1);

    var y = d3.scale.linear()
        .range([height, 0]);

    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom");

    var yAxis = d3.svg.axis()
        .scale(y)
        .orient("left")
        .tickFormat(formatPercent);

    var svg = d3.select("body").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    d3.tsv(city + "/tsv", function (error, data) {

        data.forEach(function (d) {
            d.discount = +d.discount;
        });

        x.domain(data.map(function (d) {
            return d.desc;
        }));
        y.domain([0, d3.max(data, function (d) {
            return d.discount;
        })]);

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis)
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Discount");

        svg.selectAll(".bar")
            .data(data)
            .enter().append("rect")
            .attr("class", "bar")
            .attr("x", function (d) {
                return x(d.desc);
            })
            .attr("width", x.rangeBand())
            .attr("y", function (d) {
                return y(d.discount);
            })
            .attr("height",function (d) {
                return height - y(d.discount);
            }).append("svg:title")
            .text(function (d) {
                return d.longdesc;
            });

        d3.select("input").on("change", change);

        change();

        function change() {

            var x0 = x.domain(data.sort(
                    function (a, b) {
                        return b.discount - a.discount;
                    })
                    .map(function (d) {
                        return d.desc;
                    }))
                .copy();

            var transition = svg.transition().duration(750),
                delay = function (d, i) {
                    return i * 50;
                };

            transition.selectAll(".bar")
                .delay(delay)
                .attr("x", function (d) {
                    return x0(d.desc);
                });

            transition.select(".x.axis")
                .call(xAxis)
                .selectAll("g")
                .delay(delay);
        }
    });
}
