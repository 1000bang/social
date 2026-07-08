import type { ChartBucket } from "../api/types";

export function SimpleBarChart({ data }: { data: ChartBucket[] }) {
	const max = Math.max(...data.map((d) => d.count), 1);

	return (
		<div className="simple-bar-chart">
			{data.map((d) => (
				<div className="simple-bar-column" key={d.bucket}>
					<div className="simple-bar-value">{d.count}</div>
					<div
						className="simple-bar"
						style={{ height: `${(d.count / max) * 100}%` }}
						title={`${d.bucket}: ${d.count}건`}
					/>
					<div className="simple-bar-label">{d.bucket}</div>
				</div>
			))}
		</div>
	);
}
