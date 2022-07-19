import ChevronLeft from '@mui/icons-material/ChevronLeft';
import { 
    Show,
    SimpleShowLayout,
	TopToolbar,
	ListButton,
	useRecordContext
} from 'react-admin';
import ReportChart from './ReportChart';

const ReportShowTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	return <span>Report from EISS™Cube - {record.cubeName}</span>;
};

const ReportShowActions = () => (
	<TopToolbar>
		<ListButton label="Back" icon={<ChevronLeft />} />
	</TopToolbar>
);

const ReportShow = (props: any) => {
    return (
		<Show title={<ReportShowTitle />}
			actions={<ReportShowActions />}
		>
			<SimpleShowLayout>
				<ReportChart />
			</SimpleShowLayout>
		</Show>
	);
};

export default ReportShow;
