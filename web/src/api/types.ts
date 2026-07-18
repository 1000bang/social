export interface PageResponse<T> {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export type MessageType = "TEXT" | "IMAGE" | "BUTTON";
export type AudienceType = "FOLLOWER" | "NON_FOLLOWER";
export type SendResult = "SUCCESS" | "FAILED";
export type ChartGranularity = "HOUR" | "DAY" | "MONTH";

export interface ButtonInput {
	title: string;
	url: string;
}

export interface MessageInput {
	messageType: MessageType;
	textContent?: string;
	imageUrl?: string;
	buttons?: ButtonInput[];
}

export interface CreateTemplateRequest {
	name: string;
	postId: number;
	dispatchTime?: string | null;
	keywords?: string[];
	dmKeyword?: string;
	commentReplyText?: string;
	nonKeywordCommentReplyText?: string;
	nonKeywordReplyEnabled: boolean;
	followerMessages?: MessageInput[];
	nonFollowerMessages?: MessageInput[];
}

export interface TemplateResponse {
	id: number;
	name: string;
	postId: number;
	dispatchTime: string | null;
	keywords: string[];
	dmKeyword: string | null;
	commentReplyText: string | null;
	nonKeywordCommentReplyText: string | null;
	nonKeywordReplyEnabled: boolean;
	activeYn: boolean;
	createdAt: string;
}

export interface TemplateDetailResponse {
	id: number;
	name: string;
	postId: number;
	dispatchTime: string | null;
	keywords: string[];
	dmKeyword: string | null;
	commentReplyText: string | null;
	nonKeywordCommentReplyText: string | null;
	nonKeywordReplyEnabled: boolean;
	activeYn: boolean;
	followerMessages: MessageInput[];
	nonFollowerMessages: MessageInput[];
	createdAt: string;
}

export interface SendLogResponse {
	id: number;
	templateId: number;
	templateName: string;
	audienceType: AudienceType | null;
	recipientPlatformUserId: string;
	recipientUsername: string | null;
	result: SendResult;
	failureReason: string | null;
	createdAt: string;
}

export interface SendLogSummaryResponse {
	contactedUsersThisMonth: number;
	messagesSentThisMonth: number;
}

export interface ChartBucket {
	bucket: string;
	count: number;
}

export interface SendLogInsightResponse {
	text: string;
}

export type AccountStatus = "ACTIVE" | "NEEDS_REAUTH" | "DISABLED";

export interface AccountMeResponse {
	username: string;
	profilePictureUrl: string | null;
	status: AccountStatus;
}

export interface TemplateRankingResponse {
	templateId: number;
	templateName: string;
	contactedUsers: number;
	messagesSent: number;
}

export interface PostResponse {
	id: number;
	platformPostId: string;
	caption: string | null;
	mediaType: string | null;
	thumbnailUrl: string | null;
	permalink: string | null;
	timestamp: string | null;
}

export interface MediaUploadResponse {
	id: number;
	url: string;
}

export interface AccountSettingsResponse {
	commentReplyText: string | null;
	nonKeywordCommentReplyText: string | null;
	nonFollowerMessageText: string | null;
	postPickerLimit: number;
	maxMessagesPerAudience: number;
}

export interface UpdateAccountSettingsRequest {
	commentReplyText?: string | null;
	nonKeywordCommentReplyText?: string | null;
	nonFollowerMessageText?: string | null;
	postPickerLimit: number;
	maxMessagesPerAudience: number;
}

export interface RecoveryCommentResponse {
	commentId: string;
	authorUsername: string | null;
	text: string;
	timestamp: string;
}

export interface RecoveryCardResponse {
	postId: number;
	templateId: number;
	templateName: string;
	thumbnailUrl: string | null;
	comments: RecoveryCommentResponse[];
}
