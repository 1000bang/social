import type { ChartBucket } from "../api/types";

const MAX_VISIBLE_LABELS = 12;

export function SimpleBarChart({ data }: { data: ChartBucket[] }) {
	const max = Math.max(...data.map((d) => d.count), 1);
	const labelStep = Math.ceil(data.length / MAX_VISIBLE_LABELS);

	return (
		<div className="simple-bar-chart">
			{data.map((d, index) => (
				<div className="simple-bar-column" key={d.bucket}>
					<div className="simple-bar-value">{d.count}</div>
					<div
						className="simple-bar"
						style={{ height: `${(d.count / max) * 100}%` }}
						title={`${d.bucket}: ${d.count}건`}
					/>
					<div className="simple-bar-label">{index % labelStep === 0 ? d.bucket : ""}</div>
				</div>
			))}
		</div>
	);
}
