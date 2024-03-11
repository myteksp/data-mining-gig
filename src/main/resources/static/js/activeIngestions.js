$(function () {
    API.uploads.listUnfinishedUploads(function (data) {
        for (var i = 0; i < data.length; i++) {
            const percent = data[i].processed * 100 / data[i].outOf;
            $("#activeIngestionsTable tbody").append(`
                <tr><td>${data[i].fileName}</td>
                <td><div class="progress" role="progressbar" aria-label="${data[i].fileName}" aria-valuenow="${percent}" aria-valuemin="0" aria-valuemax="100">
                <div class="progress-bar overflow-visible text-dark" style="width: ${percent}%">${percent.toFixed(2)}%</div>
                </div></td>
                </tr>
            `);
        }
    });
});