import ChevronLeft from '@material-ui/icons/ChevronLeft';
import { 
    Show,
    SimpleShowLayout,
	TopToolbar,
	ListButton
} from 'react-admin';
import { NavReportTitle } from '../common';
import ReportChart from './ReportChart';

const ReportShowActions = ({ basePath }: any) => {
    return (
        <TopToolbar>
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
        </TopToolbar>
    );
};

const ReportShow = (props: any) => {
    return (
		<Show {...props}
			title={<NavReportTitle title='Report from' />} 
			actions={<ReportShowActions {...props} />}
		>
			<SimpleShowLayout>
				<ReportChart {...props} />
			</SimpleShowLayout>
		</Show>
	);
};

export default ReportShow;
